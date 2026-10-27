"use strict";

// ─── JWT en el navegador (HS256) ─────────────────────────────────────────────

function b64url(input) {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let bin = "";
  bytes.forEach(b => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function signJwt(secret, issuer, subject, role, ttl = 3600) {
  const header  = { alg: "HS256", typ: "JWT" };
  const now     = Math.floor(Date.now() / 1000);
  const payload = { sub: subject, role, iss: issuer, iat: now, exp: now + ttl };
  const input   = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(payload))}`;
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(input));
  return `${input}.${b64url(new Uint8Array(sig))}`;
}

function decodeJwtPayload(token) {
  try {
    const part = token.split(".")[1];
    return JSON.parse(atob(part.replace(/-/g, "+").replace(/_/g, "/")));
  } catch { return null; }
}

// ─── Estado global ───────────────────────────────────────────────────────────

const state = {
  token:     null,  // access token (STUDENT or signed)
  userId:    null,  // UUID del sujeto
  problemId: null,  // UUID del problema creado
  tcCount:   0,
  ws:        null,
  submissions: new Map(), // id → { status, verdict, executionTimeMs, memoryUsedKb, language, problemId }
};

// ─── DOM helpers ─────────────────────────────────────────────────────────────

const $ = id => document.getElementById(id);

function log(msg) {
  const el = $("log");
  const ts = new Date().toLocaleTimeString();
  el.textContent += `[${ts}] ${msg}\n`;
  el.scrollTop = el.scrollHeight;
}

function setDot(id, state) {
  const el = $(id);
  el.className = `dot dot-${state}`;
}

// ─── Tabs ────────────────────────────────────────────────────────────────────

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    const panel = btn.closest(".panel");
    panel.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    panel.querySelectorAll(".tab-pane").forEach(p => p.classList.remove("active"));
    btn.classList.add("active");
    const target = $(btn.dataset.tab);
    if (target) target.classList.add("active");
  });
});

// ─── Health check ─────────────────────────────────────────────────────────────

async function checkHealth() {
  log("Verificando servicios…");
  try {
    const r = await fetch("/api/health");
    const data = await r.json();
    const map = { auth: "dot-auth", problem: "dot-problem", submission: "dot-submission", judge: "dot-judge" };
    for (const [svc, dotId] of Object.entries(map)) {
      const s = data[svc] || "down";
      setDot(dotId, s);
      log(`${svc}: ${s}`);
    }
  } catch (e) {
    log(`Error al verificar servicios: ${e.message}`);
  }
}

$("btn-health").addEventListener("click", checkHealth);

// ─── Auth helpers ─────────────────────────────────────────────────────────────

function applyToken(token) {
  state.token = token;
  const payload = decodeJwtPayload(token);
  state.userId = payload?.sub || null;

  $("display-token-short").textContent = token.slice(0, 40) + "…";
  $("display-user-id").textContent = state.userId || "desconocido";
  $("auth-result").hidden = false;

  // Auto-fill downstream
  if (state.userId) {
    $("prob-created-by").value = state.userId;
    $("sub-team-id").value     = state.userId;
    $("manual-user-id").value  = state.userId;
  }
}

$("btn-copy-token").addEventListener("click", () => {
  navigator.clipboard?.writeText(state.token).catch(() => {});
  log("Token copiado al portapapeles.");
});

// ─── Login ────────────────────────────────────────────────────────────────────

$("btn-login").addEventListener("click", async () => {
  const email = $("login-email").value.trim();
  const pass  = $("login-pass").value;
  if (!email || !pass) { log("Completa email y contraseña."); return; }

  $("login-status").className = "status status-connecting";
  $("login-status").textContent = "Conectando…";

  try {
    const res = await fetch("/api/proxy/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password: pass }),
    });
    const data = await res.json();
    if (!res.ok) {
      const msg = data.message || JSON.stringify(data);
      $("login-status").className = "status status-error";
      $("login-status").textContent = `Error ${res.status}`;
      log(`Login fallido (${res.status}): ${msg}`);
      return;
    }
    applyToken(data.accessToken);
    $("login-status").className = "status status-online";
    $("login-status").textContent = `Sesión activa (${data.type || "Bearer"})`;
    log(`Login OK. userId=${state.userId}`);
  } catch (e) {
    $("login-status").className = "status status-error";
    $("login-status").textContent = "Sin conexión";
    log(`Login error: ${e.message}`);
  }
});

// ─── Register ─────────────────────────────────────────────────────────────────

$("btn-register").addEventListener("click", async () => {
  const body = {
    firstName:   $("reg-fname").value.trim(),
    lastName:    $("reg-lname").value.trim(),
    username:    $("reg-username").value.trim(),
    email:       $("reg-email").value.trim(),
    password:    $("reg-pass").value,
    institution: $("reg-inst").value.trim(),
  };
  if (!body.username || !body.email || !body.password) {
    log("Completa username, email y contraseña."); return;
  }

  $("register-status").className = "status status-connecting";
  $("register-status").textContent = "Registrando…";

  try {
    const res = await fetch("/api/proxy/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
      const msg = data.message || JSON.stringify(data);
      $("register-status").className = "status status-error";
      $("register-status").textContent = `Error ${res.status}`;
      log(`Registro fallido (${res.status}): ${msg}`);
      return;
    }
    $("register-status").className = "status status-online";
    $("register-status").textContent = "Registrado ✓";
    log(`Registro OK. Ahora haz login con ${body.email}.`);
    $("login-email").value = body.email;
    $("login-pass").value  = body.password;
  } catch (e) {
    $("register-status").className = "status status-error";
    $("register-status").textContent = "Sin conexión";
    log(`Registro error: ${e.message}`);
  }
});

// ─── JWT manual ───────────────────────────────────────────────────────────────

$("btn-manual-uuid").addEventListener("click", () => {
  $("manual-user-id").value = crypto.randomUUID();
});

$("btn-manual-sign").addEventListener("click", async () => {
  const secret  = $("jwt-secret").value.trim();
  const issuer  = $("jwt-issuer").value.trim();
  const userId  = $("manual-user-id").value.trim();
  const role    = $("manual-role").value;
  const ttl     = Number($("jwt-ttl").value) || 3600;

  if (!secret) { log("Falta JWT_SECRET en el panel 1."); return; }
  if (!userId) { log("Genera o escribe un userId."); return; }

  try {
    const token = await signJwt(secret, issuer, userId, role, ttl);
    applyToken(token);
    log(`JWT manual firmado. sub=${userId} role=${role}`);
  } catch (e) {
    log(`Error al firmar JWT: ${e.message}`);
  }
});

// ─── Helpers para peticiones autenticadas ─────────────────────────────────────

async function authedFetch(url, opts = {}) {
  if (!state.token) throw new Error("Sin token. Completa el panel de Auth primero.");
  return fetch(url, {
    ...opts,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${state.token}`,
      ...(opts.headers || {}),
    },
  });
}

async function adminFetch(url, opts = {}) {
  const secret = $("jwt-secret").value.trim();
  const issuer = $("jwt-issuer").value.trim();
  const userId = state.userId || crypto.randomUUID();
  const ttl    = Number($("jwt-ttl").value) || 3600;

  if (!secret) throw new Error("Falta JWT_SECRET en el panel 1.");

  // Comprueba si el token actual ya tiene rol ADMIN/ORGANIZER
  if (state.token) {
    const payload = decodeJwtPayload(state.token);
    if (payload && (payload.role === "ADMIN" || payload.role === "ORGANIZER")) {
      return authedFetch(url, opts);
    }
  }

  // Firma un JWT de ADMIN automáticamente
  const adminToken = await signJwt(secret, issuer, userId, "ADMIN", ttl);
  return fetch(url, {
    ...opts,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${adminToken}`,
      ...(opts.headers || {}),
    },
  });
}

// ─── Panel 3: Crear problema ──────────────────────────────────────────────────

$("btn-sync-creator").addEventListener("click", () => {
  if (state.userId) $("prob-created-by").value = state.userId;
});

$("btn-create-problem").addEventListener("click", async () => {
  const body = {
    createdBy:    $("prob-created-by").value.trim() || crypto.randomUUID(),
    title:        $("prob-title").value.trim(),
    statement:    $("prob-statement").value.trim(),
    timeLimitMs:  Number($("prob-time").value)   || 2000,
    memoryLimitKb:Number($("prob-memory").value) || 65536,
    difficult:    Number($("prob-diff").value)   || 1,
    inputFormat:  "",
    outputFormat: "",
  };
  if (!body.title) { log("El título del problema es obligatorio."); return; }

  log("Creando problema…");
  try {
    const res = await adminFetch("/api/proxy/problems", {
      method: "POST",
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
      log(`Crear problema fallido (${res.status}): ${data.message || JSON.stringify(data)}`);
      return;
    }
    state.problemId = data.id;
    $("display-problem-id").textContent = data.id;
    $("sub-problem-id").value = data.id;
    $("problem-result").hidden = false;
    $("tc-order").value = "1";
    log(`Problema creado. ID=${data.id}`);
  } catch (e) {
    log(`Error al crear problema: ${e.message}`);
  }
});

$("btn-copy-problem-id").addEventListener("click", () => {
  navigator.clipboard?.writeText(state.problemId).catch(() => {});
  log("Problem ID copiado.");
});

// ─── Panel 3: Agregar caso de prueba ─────────────────────────────────────────

$("btn-add-tc").addEventListener("click", async () => {
  const problemId = state.problemId;
  if (!problemId) { log("Crea un problema primero."); return; }

  const body = {
    orderIndex:     Number($("tc-order").value) || 1,
    input:          $("tc-input").value,
    output:         $("tc-output").value,
    expectedOutput: $("tc-output").value,
    isSample:       $("tc-sample").checked,
  };

  log(`Agregando caso de prueba #${body.orderIndex}…`);
  try {
    const res = await adminFetch(`/api/proxy/problems/test-cases/${problemId}`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
      log(`Agregar TC fallido (${res.status}): ${data.message || JSON.stringify(data)}`);
      return;
    }
    state.tcCount++;
    $("tc-order").value = String(state.tcCount + 1);
    renderTestCase(body, data.id);
    log(`Caso de prueba ${data.id} agregado.`);
  } catch (e) {
    log(`Error al agregar TC: ${e.message}`);
  }
});

function renderTestCase(body, id) {
  const list = $("tc-list");
  const item = document.createElement("div");
  item.className = "tc-item";
  item.innerHTML = `
    <div>
      <div class="tc-label">Entrada</div>
      <pre>${escapeHtml(body.input)}</pre>
    </div>
    <div>
      <div class="tc-label">Salida esperada</div>
      <pre>${escapeHtml(body.expectedOutput)}</pre>
    </div>
    <div style="display:flex;flex-direction:column;gap:4px;align-items:flex-end;">
      <span class="badge ${body.isSample ? "badge-sample" : "badge-hidden"}">${body.isSample ? "muestra" : "oculto"}</span>
      <span style="font-size:11px;color:var(--text-dim);">#${body.orderIndex}</span>
    </div>`;
  list.appendChild(item);
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// ─── Panel 4: Enviar código ───────────────────────────────────────────────────

const SNIPPETS = {
  PYTHON: `a, b = map(int, input().split())\nprint(a + b)`,
  C: `#include <stdio.h>\nint main() {\n    int a, b;\n    scanf("%d %d", &a, &b);\n    printf("%d\\n", a + b);\n    return 0;\n}`,
  CPP: `#include <iostream>\nusing namespace std;\nint main() {\n    int a, b;\n    cin >> a >> b;\n    cout << a + b << endl;\n    return 0;\n}`,
  JAVA: `import java.util.Scanner;\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt(), b = sc.nextInt();\n        System.out.println(a + b);\n    }\n}`,
};

$("sub-language").addEventListener("change", () => {
  const lang = $("sub-language").value;
  if (SNIPPETS[lang]) $("sub-code").value = SNIPPETS[lang];
});

$("btn-sync-team").addEventListener("click", () => {
  if (state.userId) $("sub-team-id").value = state.userId;
});

$("btn-sync-problem").addEventListener("click", () => {
  if (state.problemId) $("sub-problem-id").value = state.problemId;
});

$("btn-submit").addEventListener("click", async () => {
  const body = {
    teamId:     $("sub-team-id").value.trim(),
    problemId:  $("sub-problem-id").value.trim(),
    language:   $("sub-language").value,
    sourceCode: $("sub-code").value,
  };
  if (!body.teamId || !body.problemId || !body.sourceCode) {
    log("Completa teamId, problemId y código fuente."); return;
  }
  if (!state.token) { log("Sin token. Completa el panel Auth primero."); return; }

  log("Enviando código…");
  try {
    const res = await authedFetch("/api/proxy/submissions", {
      method: "POST",
      body: JSON.stringify(body),
    });
    const data = await res.json();
    if (!res.ok) {
      log(`Envío fallido (${res.status}): ${data.message || JSON.stringify(data)}`);
      return;
    }
    const subId = data.id;
    log(`Envío creado. ID=${subId} status=${data.status}`);
    $("display-submission-id").textContent = subId;
    $("submission-result").hidden = false;
    updateSubmissionBadge("submission-badge", data.status, null);

    upsertCard(subId, {
      status: data.status,
      language: body.language,
      problemId: body.problemId,
    });

    startPolling(subId);
  } catch (e) {
    log(`Error al enviar: ${e.message}`);
  }
});

// Sondeo manual desde el botón "Sondear estado"
$("btn-poll").addEventListener("click", async () => {
  const subId = $("display-submission-id").textContent;
  if (!subId || subId === "—") { log("No hay submission ID todavía."); return; }
  await pollOnce(subId);
});

async function pollOnce(subId) {
  try {
    const res = await authedFetch(`/api/proxy/submissions/${subId}`);
    const data = await res.json();
    if (!res.ok) {
      log(`Sondeo fallido (${res.status}): ${data.message || JSON.stringify(data)}`);
      return null;
    }
    log(`Sondeo ${subId}: status=${data.status}${data.verdict ? " verdict=" + data.verdict : ""}`);
    updateSubmissionBadge("submission-badge", data.status, data.verdict);
    upsertCard(subId, {
      status: data.status,
      verdict: data.verdict,
      executionTimeMs: data.executionTimeMs,
      memoryUsedKb: data.memoryUsedKb,
    });
    return data;
  } catch (e) {
    log(`Error en sondeo: ${e.message}`);
    return null;
  }
}

function startPolling(subId) {
  let tries = 0;
  const iv = setInterval(async () => {
    tries++;
    const data = await pollOnce(subId);
    if (!data) { clearInterval(iv); return; }
    if (data.status === "JUDGED" || tries >= 20) clearInterval(iv);
  }, 2500);
}

function updateSubmissionBadge(badgeId, status, verdict) {
  const el = $(badgeId);
  if (!el) return;
  const { cls, text } = classifySubmission(status, verdict);
  el.className = `card-badge badge-${cls}`;
  el.textContent = verdict || status || "?";
}

// ─── Submission cards ─────────────────────────────────────────────────────────

function classifySubmission(status, verdict) {
  if (status === "JUDGED") {
    if (verdict === "ACCEPTED") return { cls: "accepted", text: verdict };
    return { cls: "rejected", text: verdict || "REJECTED" };
  }
  if (status === "JUDGING")  return { cls: "judging",  text: "JUDGING" };
  if (status === "QUEUED")   return { cls: "queued",   text: "QUEUED" };
  if (status === "PENDING")  return { cls: "pending",  text: "PENDING" };
  return { cls: "error", text: status || "ERROR" };
}

function upsertCard(id, patch) {
  const existing = state.submissions.get(id) || { id, createdAt: Date.now() };
  state.submissions.set(id, { ...existing, ...patch });
  renderCards();
}

function renderCards() {
  const container = $("cards");
  if (state.submissions.size === 0) {
    container.innerHTML = '<p class="empty">Aún no hay envíos que seguir.</p>';
    return;
  }

  const items = [...state.submissions.entries()].sort((a, b) => b[1].createdAt - a[1].createdAt);
  container.innerHTML = items.map(([id, s]) => {
    const { cls, text } = classifySubmission(s.status, s.verdict);
    const metrics = (s.executionTimeMs !== undefined && s.status === "JUDGED")
      ? `<span><b>Tiempo:</b> ${s.executionTimeMs} ms · <b>Memoria:</b> ${s.memoryUsedKb} KB</span>`
      : "";
    return `
      <div class="card state-${cls}">
        <div class="card-id">${id}</div>
        <span class="card-badge badge-${cls}">${text}</span>
        <div class="details">
          ${s.language ? `<span><b>Lenguaje:</b> ${s.language}</span>` : ""}
          ${s.problemId ? `<span><b>Problema:</b> ${s.problemId.slice(0,8)}…</span>` : ""}
          ${metrics}
        </div>
      </div>`;
  }).join("");
}

// ─── Panel 5: WebSocket ───────────────────────────────────────────────────────

$("btn-ws-connect").addEventListener("click", async () => {
  const wsUrl      = $("ws-url").value.trim();
  const subProto   = $("ws-subprotocol").value.trim();

  let token = state.token;
  if (!token) {
    log("Sin token. Completa el panel Auth antes de conectar el WebSocket.");
    return;
  }

  setWsStatus("connecting", "Conectando…");

  try {
    state.ws = new WebSocket(wsUrl, [subProto, `bearer.${token}`]);
  } catch (e) {
    log(`No se pudo abrir el WebSocket: ${e.message}`);
    setWsStatus("error", "Error");
    return;
  }

  state.ws.onopen = () => {
    setWsStatus("online", "Conectado");
    $("btn-ws-connect").disabled    = true;
    $("btn-ws-disconnect").disabled = false;
    log("WebSocket abierto.");
  };

  state.ws.onmessage = event => {
    log(`WS ← ${event.data}`);
    try { handleWsEvent(JSON.parse(event.data)); } catch { /* ignorar */ }
  };

  state.ws.onclose = ev => {
    setWsStatus(ev.wasClean ? "offline" : "error", `Cerrado (${ev.code})`);
    $("btn-ws-connect").disabled    = false;
    $("btn-ws-disconnect").disabled = true;
    log(`WebSocket cerrado: ${ev.code} "${ev.reason}"`);
  };

  state.ws.onerror = () => log("Error de transporte WebSocket.");
});

$("btn-ws-disconnect").addEventListener("click", () => {
  state.ws?.close(1000, "cierre manual");
});

function setWsStatus(kind, text) {
  const el = $("ws-status");
  el.className = `status status-${kind}`;
  el.textContent = text;
}

function handleWsEvent(data) {
  if (data.type !== "SUBMISSION_STATUS_UPDATED") return;
  log(`WS status update: ${data.submissionId} → ${data.status}${data.verdict ? "/" + data.verdict : ""}`);
  upsertCard(data.submissionId, {
    status:         data.status,
    verdict:        data.verdict,
    executionTimeMs:data.executionTimeMs,
    memoryUsedKb:   data.memoryUsedKb,
    language:       data.language,
    problemId:      data.problemId,
  });

  // Actualiza también el badge del panel 4 si coincide
  const currentId = $("display-submission-id").textContent;
  if (currentId && currentId === data.submissionId) {
    updateSubmissionBadge("submission-badge", data.status, data.verdict);
  }
}

// ─── Init ─────────────────────────────────────────────────────────────────────

// Carga la configuracion del .env en los campos del panel. No se rellena nada
// desde el HTML para no dejar el JWT_SECRET escrito en un fichero del repositorio.
async function loadConfig() {
  try {
    const r = await fetch("/api/config");
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    const cfg = await r.json();

    $("jwt-secret").value      = cfg.jwtSecret;
    $("jwt-issuer").value      = cfg.jwtIssuer;
    $("jwt-ttl").value         = cfg.jwtTtlSeconds;
    $("ws-url").value          = cfg.wsUrl;
    $("ws-subprotocol").value  = cfg.wsSubprotocol;

    if (cfg.jwtSecretPresent) {
      log("Configuración cargada desde .env.");
    } else {
      log("Configuración cargada, pero falta JWT_SECRET en el .env: pégalo en el panel 1.");
    }
  } catch (err) {
    log(`No se pudo cargar /api/config (${err.message}). Rellena el panel 1 a mano.`);
  }
}

(function init() {
  $("manual-user-id").value = crypto.randomUUID();
  loadConfig();
  checkHealth();
})();
