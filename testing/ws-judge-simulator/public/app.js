"use strict";

// ---------- JWT firmado en el navegador (HS256), para no depender de auth-service ----------

function base64url(bytesOrString) {
  const bytes = typeof bytesOrString === "string" ? new TextEncoder().encode(bytesOrString) : bytesOrString;
  let binary = "";
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function createJwt(secret, issuer, subject, role, ttlSeconds = 3600) {
  const header = { alg: "HS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const payload = { sub: subject, role, iss: issuer, iat: now, exp: now + ttlSeconds };

  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(signingInput));
  return `${signingInput}.${base64url(new Uint8Array(signature))}`;
}

// ---------- Estado ----------

/** @type {WebSocket|null} */
let ws = null;

/** submissionId -> { teamId, problemId, language, state, verdict, executionTimeMs, memoryUsedKb, createdAt, source, judgingUntil } */
const submissions = new Map();

let pendingPollTimer = null;
const autoJudged = new Set();

// ---------- Utilidades de DOM ----------

const $ = (id) => document.getElementById(id);

function log(line) {
  const el = $("log");
  const time = new Date().toLocaleTimeString();
  el.textContent += `[${time}] ${line}\n`;
  el.scrollTop = el.scrollHeight;
}

function shortId(id) {
  return id ? id.slice(0, 8) : "?";
}

function setConnectionStatus(kind, text) {
  const el = $("connection-status");
  el.className = `status status-${kind}`;
  el.textContent = text;
}

// ---------- Conexión WebSocket ----------

$("btn-random-user").addEventListener("click", () => {
  $("user-id").value = crypto.randomUUID();
});

$("btn-random-problem").addEventListener("click", () => {
  $("problem-id").value = crypto.randomUUID();
});

$("btn-sync-team").addEventListener("click", () => {
  $("team-id").value = $("user-id").value;
});

$("btn-connect").addEventListener("click", async () => {
  const wsUrl = $("ws-url").value.trim();
  const subProtocol = $("ws-subprotocol").value.trim();
  const secret = $("jwt-secret").value;
  const issuer = $("jwt-issuer").value.trim();
  const userId = $("user-id").value.trim();
  const role = $("user-role").value;
  const useQueryToken = $("use-query-token").checked;

  if (!secret) {
    alert("Pega el JWT_SECRET de backend/.env; sin él no se puede firmar un token válido.");
    return;
  }
  if (!userId) {
    alert("Genera o escribe un userId (UUID) antes de conectar.");
    return;
  }

  setConnectionStatus("connecting", "Conectando…");

  let token;
  try {
    token = await createJwt(secret, issuer, userId, role);
  } catch (err) {
    log(`No se pudo firmar el token: ${err.message}`);
    setConnectionStatus("error", "Error de token");
    return;
  }

  let url = wsUrl;
  let protocols = [subProtocol, `bearer.${token}`];
  if (useQueryToken) {
    const separator = wsUrl.includes("?") ? "&" : "?";
    url = `${wsUrl}${separator}token=${encodeURIComponent(token)}`;
    protocols = [subProtocol];
  }

  try {
    ws = new WebSocket(url, protocols);
  } catch (err) {
    log(`No se pudo abrir el WebSocket: ${err.message}`);
    setConnectionStatus("error", "Error");
    return;
  }

  ws.onopen = () => {
    setConnectionStatus("online", `Conectado como ${shortId(userId)} (${role})`);
    $("btn-connect").disabled = true;
    $("btn-disconnect").disabled = false;
    log("WebSocket abierto.");
  };

  ws.onmessage = (event) => {
    log(`Mensaje recibido: ${event.data}`);
    try {
      handleStatusEvent(JSON.parse(event.data));
    } catch (err) {
      log(`No se pudo interpretar el mensaje: ${err.message}`);
    }
  };

  ws.onclose = (event) => {
    setConnectionStatus(event.wasClean ? "offline" : "error", `Cerrado (código ${event.code})`);
    $("btn-connect").disabled = false;
    $("btn-disconnect").disabled = true;
    log(`WebSocket cerrado: código ${event.code}, motivo "${event.reason || "sin motivo"}".`);
  };

  ws.onerror = () => {
    log("Error de transporte en el WebSocket (revisa la consola del navegador).");
  };
});

$("btn-disconnect").addEventListener("click", () => {
  if (ws) ws.close(1000, "cierre manual desde el probador");
});

// ---------- Manejo de eventos de estado ----------

function classifyState(status, verdict) {
  if (status === "JUDGED") {
    return verdict === "ACCEPTED" ? "accepted" : "rejected";
  }
  if (status === "SYSTEM_ERROR") return "system-error";
  if (status === "QUEUED" || status === "PENDING") return "queued";
  return "queued";
}

function labelFor(state, verdict) {
  switch (state) {
    case "created":
      return "Creado";
    case "queued":
      return "En cola";
    case "judging":
      return "Juzgando…";
    case "accepted":
      return "ACCEPTED";
    case "rejected":
      return verdict || "Rechazado";
    case "system-error":
      return "SYSTEM_ERROR";
    default:
      return state;
  }
}

function upsertSubmission(id, patch) {
  const existing = submissions.get(id) || {
    teamId: "?",
    problemId: "?",
    language: "?",
    state: "created",
    createdAt: Date.now(),
    source: "desconocido",
  };
  submissions.set(id, { ...existing, ...patch });
  renderCards();
}

function handleStatusEvent(data) {
  if (data.type !== "SUBMISSION_STATUS_UPDATED") return;

  upsertSubmission(data.submissionId, {
    teamId: data.teamId,
    problemId: data.problemId,
    state: classifyState(data.status, data.verdict),
    verdict: data.verdict,
    executionTimeMs: data.executionTimeMs,
    memoryUsedKb: data.memoryUsedKb,
    judgingUntil: null,
  });
}

function renderCards() {
  const container = $("cards");
  if (submissions.size === 0) {
    container.innerHTML = '<p class="empty">Aún no hay envíos que seguir.</p>';
    return;
  }

  const items = [...submissions.entries()].sort((a, b) => b[1].createdAt - a[1].createdAt);

  container.innerHTML = items
    .map(([id, s]) => {
      const remaining = s.judgingUntil ? Math.max(0, Math.round((s.judgingUntil - Date.now()) / 1000)) : null;
      return `
      <div class="card state-${s.state}">
        <div class="card-id">${id}</div>
        <span class="badge badge-${s.state}">${labelFor(s.state, s.verdict)}${
        remaining !== null ? ` (${remaining}s)` : ""
      }</span>
        <div class="details">
          <span><b>Equipo:</b> ${shortId(s.teamId)}</span>
          <span><b>Problema:</b> ${shortId(s.problemId)}</span>
          <span><b>Lenguaje:</b> ${s.language || "?"}</span>
          <span><b>Origen:</b> ${s.source}</span>
          ${
            s.executionTimeMs !== undefined && s.state !== "queued" && s.state !== "created"
              ? `<span><b>Tiempo:</b> ${s.executionTimeMs} ms · <b>Memoria:</b> ${s.memoryUsedKb} KB</span>`
              : ""
          }
        </div>
      </div>`;
    })
    .join("");
}

setInterval(() => {
  if ([...submissions.values()].some((s) => s.judgingUntil)) renderCards();
}, 1000);

// ---------- Crear envíos ----------

function submissionApiBase() {
  const wsUrl = new URL($("ws-url").value.trim());
  const httpProtocol = wsUrl.protocol === "wss:" ? "https:" : "http:";
  return `${httpProtocol}//${wsUrl.host}`;
}

$("btn-create-real").addEventListener("click", async () => {
  const secret = $("jwt-secret").value;
  const issuer = $("jwt-issuer").value.trim();
  const userId = $("user-id").value.trim();
  const role = $("user-role").value;
  if (!secret || !userId) {
    alert("Completa el secreto JWT y el userId en la sección 1 antes de crear envíos.");
    return;
  }

  const count = Math.max(1, Number($("batch-count").value) || 1);
  const token = await createJwt(secret, issuer, userId, role);
  const base = submissionApiBase();

  for (let i = 0; i < count; i++) {
    const body = {
      teamId: $("team-id").value.trim(),
      problemId: $("problem-id").value.trim(),
      language: $("language").value,
      sourceCode: $("source-code").value,
    };

    try {
      const res = await fetch(`${base}/api/v1/submissions`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        log(`POST /api/v1/submissions → ${res.status} ${res.statusText}. (Ver aviso sobre el filtro JWT HTTP.)`);
        continue;
      }

      const created = await res.json();
      upsertSubmission(created.id, {
        teamId: body.teamId,
        problemId: body.problemId,
        language: body.language,
        state: "queued",
        source: "API real",
        createdAt: Date.now(),
      });
      log(`Envío creado vía API real: ${created.id}`);
    } catch (err) {
      log(`Fallo llamando a la API real: ${err.message}`);
    }
  }
});

// ---------- Panel del juez simulado ----------

function verdictOptionsHtml() {
  const verdicts = [
    "ACCEPTED",
    "WRONG_ANSWER",
    "TIME_LIMIT_EXCEEDED",
    "MEMORY_LIMIT_EXCEEDED",
    "RUNTIME_ERROR",
    "COMPILATION_ERROR",
  ];
  return verdicts.map((v) => `<option value="${v}">${v}</option>`).join("");
}

async function sendVerdict(simulatorUrl, id, body) {
  const res = await fetch(`${simulatorUrl}/api/judge/${id}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const data = await res.json();
  if (!res.ok) {
    log(`Harness /api/judge/${shortId(id)} → ${res.status}: ${data.error}`);
    return;
  }
  upsertSubmission(id, { state: "judging", judgingUntil: Date.now() + data.etaMs });
  log(`Veredicto ${body.verdict} programado para ${shortId(id)} en ${data.etaMs} ms.`);
}

async function sendRandomVerdict(simulatorUrl, id) {
  const res = await fetch(`${simulatorUrl}/api/judge/${id}/random`, { method: "POST" });
  const data = await res.json();
  if (!res.ok) {
    log(`Harness /api/judge/${shortId(id)}/random → ${res.status}: ${data.error}`);
    return;
  }
  upsertSubmission(id, { state: "judging", judgingUntil: Date.now() + data.etaMs });
  log(`Veredicto aleatorio ${data.verdict} programado para ${shortId(id)} en ${data.etaMs} ms.`);
}

function buildPendingRow(item, simulatorUrl) {
  const row = document.createElement("div");
  row.className = "pending-item";
  row.dataset.id = item.submissionId;
  row.innerHTML = `
    <div class="meta">
      <strong>${shortId(item.submissionId)}</strong> · ${item.language} ·
      <span class="waiting-time"></span>
      <span class="judging-badge" hidden> · juzgando…</span>
    </div>
    <div class="pending-controls">
      <select class="verdict-select">${verdictOptionsHtml()}</select>
      <input class="exec-time" type="number" value="150" min="0" title="Tiempo de ejecución (ms)" />
      <input class="mem-used" type="number" value="2048" min="0" title="Memoria usada (KB)" />
      <input class="delay-ms" type="number" value="2000" min="0" step="500" title="Tiempo simulado del juez (ms)" />
      <button type="button" class="btn-send-verdict">Enviar veredicto</button>
      <button type="button" class="btn-random-verdict">Aleatorio</button>
    </div>`;

  row.querySelector(".btn-send-verdict").addEventListener("click", () => {
    sendVerdict(simulatorUrl, item.submissionId, {
      verdict: row.querySelector(".verdict-select").value,
      executionTimeMs: Number(row.querySelector(".exec-time").value),
      memoryUsedKb: Number(row.querySelector(".mem-used").value),
      delayMs: Number(row.querySelector(".delay-ms").value),
    });
  });

  row.querySelector(".btn-random-verdict").addEventListener("click", () => {
    sendRandomVerdict(simulatorUrl, item.submissionId);
  });

  return row;
}

// Actualiza la lista de pendientes sin reconstruir el DOM de las filas que ya
// existían: si se reemplazara el <select> de veredicto en cada sondeo (cada
// 1.5s), el navegador cerraría cualquier desplegable abierto y perdería la
// selección que el usuario acababa de hacer.
function renderPending(items) {
  const container = $("pending-list");
  const currentIds = new Set(items.map((item) => item.submissionId));

  container.querySelectorAll(".pending-item").forEach((row) => {
    if (!currentIds.has(row.dataset.id)) row.remove();
  });

  if (items.length === 0) {
    if (!container.querySelector(".pending-item")) {
      container.innerHTML = '<p class="empty">Sin envíos esperando veredicto todavía.</p>';
    }
    return;
  }

  container.querySelector(".empty")?.remove();
  const simulatorUrl = $("simulator-url").value.trim().replace(/\/$/, "");

  items.forEach((item) => {
    let row = container.querySelector(`.pending-item[data-id="${item.submissionId}"]`);
    if (!row) {
      row = buildPendingRow(item, simulatorUrl);
      container.appendChild(row);
    }

    const waitingSeconds = Math.round((Date.now() - item.receivedAt) / 1000);
    row.querySelector(".waiting-time").textContent = `esperando ${waitingSeconds}s`;
    row.classList.toggle("judging", item.judging);
    row.querySelector(".judging-badge").hidden = !item.judging;
    row.querySelector(".btn-send-verdict").disabled = item.judging;
    row.querySelector(".btn-random-verdict").disabled = item.judging;
  });
}

async function pollPending() {
  const simulatorUrl = $("simulator-url").value.trim().replace(/\/$/, "");
  try {
    const res = await fetch(`${simulatorUrl}/api/pending`);
    const items = await res.json();
    renderPending(items);

    if ($("auto-judge").checked) {
      for (const item of items) {
        if (!item.judging && !autoJudged.has(item.submissionId)) {
          autoJudged.add(item.submissionId);
          sendRandomVerdict(simulatorUrl, item.submissionId);
        }
      }
    }
  } catch (err) {
    // El simulador puede no estar corriendo todavía; no llenamos el log de ruido.
  }
}

pendingPollTimer = setInterval(pollPending, 1500);
pollPending();

// ---------- Inicialización ----------

(function init() {
  const userId = crypto.randomUUID();
  $("user-id").value = userId;
  $("team-id").value = userId;
  $("problem-id").value = crypto.randomUUID();
})();
