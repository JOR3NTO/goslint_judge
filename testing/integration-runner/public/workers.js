"use strict";

// Prueba de workers: lanza N envíos a la vez y mide, por cada uno, cuánto espera
// en la cola y cuánto tarda en evaluarse. Con eso calcula el pico de
// evaluaciones simultáneas, que debe coincidir con los workers del juez.

// ─── JWT en el navegador (HS256), igual que en app.js ────────────────────────

function b64url(input) {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let bin = "";
  bytes.forEach(b => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function signJwt(secret, issuer, subject, role, ttl = 3600) {
  const now     = Math.floor(Date.now() / 1000);
  const payload = { sub: subject, role, iss: issuer, iat: now, exp: now + ttl };
  const input   = `${b64url(JSON.stringify({ alg: "HS256", typ: "JWT" }))}.${b64url(JSON.stringify(payload))}`;
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(input));
  return `${input}.${b64url(new Uint8Array(sig))}`;
}

// ─── Soluciones de prueba ────────────────────────────────────────────────────
// El problema pide imprimir el doble del número leído.

const FAST = {
  PYTHON: `n = int(input())\nprint(n * 2)\n`,
  C:      `#include <stdio.h>\nint main() {\n    long n;\n    scanf("%ld", &n);\n    printf("%ld\\n", n * 2);\n    return 0;\n}\n`,
  CPP:    `#include <iostream>\nint main() {\n    long n;\n    std::cin >> n;\n    std::cout << n * 2 << std::endl;\n    return 0;\n}\n`,
  JAVA:   `import java.util.Scanner;\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.println(sc.nextLong() * 2);\n    }\n}\n`,
};

// Misma respuesta, pero quemando CPU antes: así las evaluaciones duran lo
// bastante como para ver cuáles coinciden en el tiempo.
const BURN = {
  PYTHON: `n = int(input())\nx = 0\nfor i in range(12000000):\n    x += i\nprint(n * 2)\n`,
  C:      `#include <stdio.h>\nint main() {\n    long n;\n    volatile unsigned long x = 0;\n    scanf("%ld", &n);\n    for (long i = 0; i < 300000000L; i++) x += i;\n    printf("%ld\\n", n * 2);\n    return 0;\n}\n`,
  CPP:    `#include <iostream>\nint main() {\n    long n;\n    volatile unsigned long x = 0;\n    std::cin >> n;\n    for (long i = 0; i < 300000000L; i++) x = x + i;\n    std::cout << n * 2 << std::endl;\n    return 0;\n}\n`,
  JAVA:   `import java.util.Scanner;\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        long n = sc.nextLong();\n        long x = 0;\n        for (long i = 0; i < 600000000L; i++) x += i % 7;\n        System.out.println(x >= 0 ? n * 2 : -1);\n    }\n}\n`,
};

// Un envío por cada veredicto que el juez sabe emitir.
const MIX = [
  { expected: "ACCEPTED",              source: FAST.PYTHON },
  { expected: "WRONG_ANSWER",          source: `n = int(input())\nprint(n * 3)\n` },
  { expected: "TIME_LIMIT_EXCEEDED",   source: `while True:\n    pass\n` },
  { expected: "RUNTIME_ERROR",         source: `n = int(input())\nprint(n // 0)\n` },
  { expected: "COMPILATION_ERROR",     source: `print(\n` },
  { expected: "MEMORY_LIMIT_EXCEEDED", source: `x = bytearray(600 * 1024 * 1024)\nprint(int(input()) * 2)\n` },
];

const LANGUAGES = ["PYTHON", "C", "CPP", "JAVA"];
const TEST_CASES = [["1", "2"], ["2", "4"], ["21", "42"]];

const POLL_MS = 500;       // Sondeo de respaldo, por si el WebSocket no llega.
const TICK_MS = 200;       // Refresco de las barras mientras hay envíos vivos.
const RUN_TIMEOUT_MS = 10 * 60 * 1000;

// ─── Estado ──────────────────────────────────────────────────────────────────

const config = { jwtSecret: "", jwtIssuer: "goslint-judge", jwtTtlSeconds: 3600, wsUrl: "", wsSubprotocol: "" };

// Una prueba en curso (o la última terminada).
let run = null;

const $ = id => document.getElementById(id);

function log(msg) {
  const el = $("log");
  el.textContent += `[${new Date().toLocaleTimeString()}] ${msg}\n`;
  el.scrollTop = el.scrollHeight;
}

function setStatus(id, kind, text) {
  $(id).className = `status status-${kind}`;
  $(id).textContent = text;
}

const seconds = ms => `${(ms / 1000).toFixed(1).replace(".", ",")} s`;

// ─── Servicios y configuración ───────────────────────────────────────────────

async function checkHealth() {
  try {
    const data = await (await fetch("/api/health")).json();
    for (const svc of ["auth", "problem", "submission", "judge"]) {
      $(`dot-${svc}`).className = `dot dot-${data[svc] || "down"}`;
    }
    return data;
  } catch (e) {
    log(`No se pudo verificar los servicios: ${e.message}`);
    return {};
  }
}

async function loadConfig() {
  try {
    const r = await fetch("/api/config");
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    Object.assign(config, await r.json());
    log(config.jwtSecret ? "Configuración cargada desde .env." : "Falta JWT_SECRET en el .env: la prueba no podrá firmar tokens.");
  } catch (e) {
    log(`No se pudo cargar /api/config: ${e.message}`);
  }
}

async function api(method, url, token, body) {
  const res = await fetch(url, {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
  if (!res.ok) throw new Error(`${method} ${url} → ${res.status}: ${data?.message || data?.error || text}`);
  return data;
}

// ─── Preparación del problema ────────────────────────────────────────────────

async function createProblem(adminToken, adminId) {
  const problem = await api("POST", "/api/proxy/problems", adminToken, {
    createdBy: adminId,
    title: "Doble de un número",
    statement: "Dado un entero n, imprimir 2n. Problema creado por la prueba de workers.",
    timeLimitMs: 2000,
    memoryLimitKb: 131072,
    difficult: 800,
    inputFormat: "",
    outputFormat: "",
  });
  for (let i = 0; i < TEST_CASES.length; i++) {
    const [input, output] = TEST_CASES[i];
    await api("POST", `/api/proxy/problems/test-cases/${problem.id}`, adminToken, {
      orderIndex: i + 1, input, output, expectedOutput: output, isSample: i === 0,
    });
  }
  log(`Problema creado con ${TEST_CASES.length} casos de prueba. ID=${problem.id}`);
  return problem.id;
}

// ─── Carga de trabajo ────────────────────────────────────────────────────────

function buildItems(count, workload, language) {
  const items = [];
  // submission-service rechaza con 409 un envío idéntico a otro del mismo equipo,
  // así que cada fuente lleva un comentario distinto.
  const nonce = crypto.randomUUID().slice(0, 8);
  for (let n = 1; n <= count; n++) {
    let lang = language === "ALL" ? LANGUAGES[(n - 1) % LANGUAGES.length] : language;
    let source;
    let expected = "ACCEPTED";
    if (workload === "mix") {
      const kind = MIX[(n - 1) % MIX.length];
      lang = "PYTHON";
      source = kind.source;
      expected = kind.expected;
    } else {
      source = (workload === "burn" ? BURN : FAST)[lang];
    }
    source += `${lang === "PYTHON" ? "#" : "//"} envío ${n} de la prueba ${nonce}\n`;
    items.push({
      n, language: lang, source, expected,
      id: null, status: "PENDING", verdict: null, error: null,
      sentAt: null, judgingAt: null, endedAt: null,
      executionTimeMs: null, memoryUsedKb: null, el: null,
    });
  }
  return items;
}

// ─── Seguimiento de estados ──────────────────────────────────────────────────

const RANK = { PENDING: 0, QUEUED: 1, JUDGING: 2 };
const isFinal = status => !(status in RANK);

// Los estados solo avanzan: un evento atrasado (del sondeo o del WebSocket)
// no puede devolver un envío a un estado anterior.
function applyStatus(item, update, at) {
  const status = update.status;
  if (!status || item.endedAt !== null) return;
  const rank = isFinal(status) ? 3 : RANK[status];
  const current = isFinal(item.status) ? 3 : RANK[item.status];
  if (rank < current) return;

  item.status = status;
  if (rank >= 2 && item.judgingAt === null) item.judgingAt = at;
  if (rank === 3) {
    item.endedAt = at;
    item.verdict = update.verdict || null;
    item.executionTimeMs = update.executionTimeMs ?? null;
    item.memoryUsedKb = update.memoryUsedKb ?? null;
  }
}

function onWsMessage(raw) {
  let data;
  try { data = JSON.parse(raw); } catch { return; }
  if (data.type !== "SUBMISSION_STATUS_UPDATED" || !run) return;
  const at = performance.now();
  const item = run.byId.get(data.submissionId);
  if (item) {
    applyStatus(item, data, at);
  } else {
    // El evento puede llegar antes que la respuesta del POST que nos da el id.
    const queue = run.earlyEvents.get(data.submissionId) || [];
    queue.push({ data, at });
    run.earlyEvents.set(data.submissionId, queue);
  }
}

function openWebSocket(token) {
  return new Promise(resolve => {
    let settled = false;
    const done = ok => { if (!settled) { settled = true; resolve(ok); } };
    let ws;
    try {
      ws = new WebSocket(config.wsUrl, [config.wsSubprotocol, `bearer.${token}`]);
    } catch (e) {
      log(`No se pudo abrir el WebSocket: ${e.message}`);
      return done(null);
    }
    ws.onopen = () => { setStatus("ws-status", "online", "WebSocket conectado"); done(ws); };
    ws.onmessage = event => onWsMessage(event.data);
    ws.onclose = () => { setStatus("ws-status", "offline", "WebSocket desconectado"); done(null); };
    ws.onerror = () => done(null);
    setTimeout(() => done(null), 3000);
  });
}

async function submit(item, token, teamId, problemId) {
  item.sentAt = performance.now();
  try {
    const data = await api("POST", "/api/proxy/submissions", token, {
      teamId, problemId, language: item.language, sourceCode: item.source,
    });
    item.id = data.id;
    run.byId.set(data.id, item);
    applyStatus(item, data, performance.now());
    for (const early of run.earlyEvents.get(data.id) || []) applyStatus(item, early.data, early.at);
    run.earlyEvents.delete(data.id);
  } catch (e) {
    item.error = e.message;
    item.status = "NO_ENVIADO";
    item.endedAt = performance.now();
    log(`Envío #${item.n} rechazado: ${e.message}`);
  }
}

async function pollPending(token) {
  const pending = run.items.filter(item => item.id && item.endedAt === null);
  await Promise.all(pending.map(async item => {
    try {
      const data = await api("GET", `/api/proxy/submissions/${item.id}`, token);
      applyStatus(item, data, performance.now());
    } catch { /* se reintenta en el siguiente sondeo */ }
  }));
}

// ─── Métricas ────────────────────────────────────────────────────────────────

// Máximo de evaluaciones que coinciden en el tiempo.
function peakConcurrency(items, now) {
  const events = [];
  for (const item of items) {
    if (item.judgingAt === null) continue;
    events.push([item.judgingAt, 1], [item.endedAt ?? now, -1]);
  }
  // A igual instante, primero las salidas: dos evaluaciones consecutivas no cuentan como simultáneas.
  events.sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  let current = 0, peak = 0;
  for (const [, delta] of events) {
    current += delta;
    peak = Math.max(peak, current);
  }
  return peak;
}

function verdictOf(item) {
  if (item.error) return "NO ENVIADO";
  if (item.endedAt === null) return item.status;
  return item.status === "JUDGED" ? (item.verdict || "JUDGED") : item.status;
}

function badgeClass(item) {
  if (item.error) return "error";
  if (item.endedAt === null) return item.status === "JUDGING" ? "judging" : item.status === "QUEUED" ? "queued" : "pending";
  if (item.status !== "JUDGED") return "error";
  return item.verdict === "ACCEPTED" ? "accepted" : "rejected";
}

// ─── Pintado ─────────────────────────────────────────────────────────────────

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function buildTimeline() {
  const root = $("timeline");
  root.replaceChildren();

  const axis = el("div", "tl-row tl-axis");
  axis.append(el("div", "tl-label", "Envío"), el("div", "tl-track"), el("div", "tl-summary", "Espera · evaluación · veredicto"));
  root.append(axis);
  run.axisTrack = axis.children[1];

  for (const item of run.items) {
    const row = el("div", "tl-row");
    row.tabIndex = 0;
    const track = el("div", "tl-track");
    const wait = el("div", "tl-seg seg-wait");
    const judge = el("div", "tl-seg seg-judge");
    track.append(wait, judge);
    const summary = el("div", "tl-summary");
    const times = el("span");
    const badge = el("span", "card-badge");
    summary.append(times, badge);
    row.append(el("div", "tl-label", `#${String(item.n).padStart(2, "0")} ${item.language}`), track, summary);
    row.addEventListener("pointermove", event => showTooltip(item, event.clientX, event.clientY));
    row.addEventListener("pointerleave", hideTooltip);
    row.addEventListener("focus", () => {
      const box = row.getBoundingClientRect();
      showTooltip(item, box.left + 120, box.bottom);
    });
    row.addEventListener("blur", hideTooltip);
    root.append(row);
    item.el = { wait, judge, times, badge };
  }
}

function niceStep(spanMs) {
  for (const step of [200, 500, 1000, 2000, 5000, 10000, 20000, 30000, 60000, 120000, 300000]) {
    if (spanMs / step <= 6) return step;
  }
  return 600000;
}

function render() {
  if (!run) return;
  const now = run.finishedAt ?? performance.now();
  const span = Math.max(now - run.startedAt, 1000);
  const pct = ms => `${Math.max(0, (ms / span) * 100)}%`;

  // Eje de tiempo
  const step = niceStep(span);
  run.axisTrack.replaceChildren();
  for (let t = 0; t <= span; t += step) {
    const tick = el("span", "tl-tick", seconds(t));
    tick.style.left = pct(t);
    run.axisTrack.append(tick);
  }

  let waiting = 0, judging = 0, done = 0, correct = 0;
  for (const item of run.items) {
    if (item.sentAt === null) continue;
    const start = item.sentAt - run.startedAt;
    const end = (item.endedAt ?? now) - run.startedAt;
    const judgeStart = item.judgingAt === null ? end : item.judgingAt - run.startedAt;

    item.el.wait.style.left = pct(start);
    item.el.wait.style.width = pct(judgeStart - start);
    item.el.judge.style.left = pct(judgeStart);
    item.el.judge.style.width = pct(end - judgeStart);

    const evaluation = item.judgingAt === null ? "—" : seconds(end - judgeStart);
    item.el.times.textContent = `${seconds(judgeStart - start)} · ${evaluation}`;
    const verdict = verdictOf(item);
    const mismatch = item.endedAt !== null && verdict !== item.expected;
    item.el.badge.className = `card-badge badge-${badgeClass(item)}`;
    item.el.badge.textContent = mismatch ? `${verdict} (se esperaba ${item.expected})` : verdict;

    if (item.endedAt !== null) {
      done++;
      if (!mismatch) correct++;
    } else if (item.judgingAt !== null) {
      judging++;
    } else {
      waiting++;
    }
  }

  const peak = peakConcurrency(run.items, now);
  $("t-peak").textContent = peak;
  $("t-peak-note").textContent = `de ${run.expectedWorkers} workers esperados`;
  $("t-waiting").textContent = waiting;
  $("t-judging").textContent = judging;
  $("t-done").textContent = done;
  $("t-done-note").textContent = `de ${run.items.length} envíos`;
  $("t-correct").textContent = correct;
  $("t-correct-note").textContent = `de ${done} terminados`;
  $("t-total").textContent = seconds(now - run.startedAt);

  renderCheck(peak, done, correct);
}

function renderCheck(peak, done, correct) {
  const check = $("check");
  const total = run.items.length;
  const expected = run.expectedWorkers;
  let kind = "idle", text = "Prueba en curso…";

  if (peak > expected) {
    kind = "bad";
    text = `✖ Fallo: se evaluaron ${peak} envíos a la vez, más que los ${expected} workers esperados.`;
  } else if (run.finishedAt !== null) {
    if (correct < done) {
      kind = "bad";
      text = `✖ Fallo: ${done - correct} de ${done} envíos no terminaron con el veredicto esperado.`;
    } else if (peak === expected) {
      kind = "ok";
      text = `✔ Correcto: el juez evaluó ${peak} envíos a la vez, igual que sus ${expected} workers, y el resto esperó en la cola. Los ${done} veredictos son los esperados.`;
    } else {
      kind = "warn";
      const reason = total < expected
        ? "se lanzaron menos envíos que workers"
        : run.wsConnected ? "las evaluaciones son tan cortas que apenas coinciden; prueba con la carga de CPU"
                          : "sin WebSocket el sondeo no ve las evaluaciones cortas";
      text = `⚠ Aviso: el pico fue de ${peak} evaluaciones simultáneas y se esperaban ${expected} (${reason}).`;
    }
  }
  check.className = `check check-${kind}`;
  check.textContent = text;
}

// ─── Tooltip ─────────────────────────────────────────────────────────────────

function showTooltip(item, x, y) {
  const now = run.finishedAt ?? performance.now();
  const end = item.endedAt ?? now;
  const judgeStart = item.judgingAt ?? end;
  const tip = $("tooltip");
  tip.replaceChildren(el("div", "tt-title", `Envío #${item.n} · ${item.language}`));

  const addRow = (label, value, keyClass) => {
    const row = el("div", "tt-row");
    const name = el("span");
    if (keyClass) name.append(el("span", `tt-key ${keyClass}`));
    name.append(label);
    row.append(name, el("b", null, value));
    tip.append(row);
  };
  addRow("En cola", seconds(judgeStart - item.sentAt), "seg-wait");
  addRow("Evaluándose", item.judgingAt === null ? "—" : seconds(end - judgeStart), "seg-judge");
  addRow("Estado", verdictOf(item));
  addRow("Veredicto esperado", item.expected);
  if (item.executionTimeMs !== null) addRow("CPU medida", `${item.executionTimeMs} ms`);
  if (item.memoryUsedKb !== null) addRow("Memoria", `${item.memoryUsedKb} KB`);
  if (item.id) addRow("ID", item.id);

  tip.hidden = false;
  const box = tip.getBoundingClientRect();
  tip.style.left = `${Math.min(x + 14, window.innerWidth - box.width - 8)}px`;
  tip.style.top = `${Math.min(y + 14, window.innerHeight - box.height - 8)}px`;
}

function hideTooltip() {
  $("tooltip").hidden = true;
}

// ─── Ejecución de la prueba ──────────────────────────────────────────────────

async function launch() {
  const count = Math.min(60, Math.max(1, Number($("count").value) || 1));
  const expectedWorkers = Math.max(1, Number($("expected-workers").value) || 1);
  const workload = $("workload").value;
  const language = $("language").value;

  if (!config.jwtSecret) { log("Falta JWT_SECRET: revisa backend/services/submission-service/.env."); return; }

  $("btn-run").disabled = true;
  setStatus("run-status", "connecting", "Preparando…");
  if (run?.ws) run.ws.close(1000, "nueva prueba");

  try {
    const health = await checkHealth();
    if (health.judge !== "up") log("Aviso: judge-service no responde; los envíos se quedarán en la cola.");

    // Un equipo nuevo por prueba: así el WebSocket solo recibe sus envíos.
    const userId = crypto.randomUUID();
    const studentToken = await signJwt(config.jwtSecret, config.jwtIssuer, userId, "STUDENT", config.jwtTtlSeconds);
    const adminToken = await signJwt(config.jwtSecret, config.jwtIssuer, userId, "ADMIN", config.jwtTtlSeconds);

    let problemId = $("problem-id").value.trim();
    if (!problemId) {
      problemId = await createProblem(adminToken, userId);
      $("problem-id").value = problemId;
    }

    run = {
      items: buildItems(count, workload, language),
      byId: new Map(), earlyEvents: new Map(),
      expectedWorkers, startedAt: 0, finishedAt: null,
      ws: null, wsConnected: false, axisTrack: null,
    };
    run.ws = await openWebSocket(studentToken);
    run.wsConnected = run.ws !== null;
    if (!run.wsConnected) log("Sin WebSocket: los estados se leerán solo por sondeo, cada 500 ms.");

    buildTimeline();
    log(`Lanzando ${count} envíos a la vez (carga: ${workload}, lenguaje: ${language})…`);
    setStatus("run-status", "connecting", "En curso…");
    run.startedAt = performance.now();
    const ticker = setInterval(render, TICK_MS);

    await Promise.all(run.items.map(item => submit(item, studentToken, userId, problemId)));
    log(`Los ${count} envíos están entregados; esperando al juez.`);

    while (run.items.some(item => item.endedAt === null)) {
      if (performance.now() - run.startedAt > RUN_TIMEOUT_MS) {
        log("Se agotó el tiempo de la prueba con envíos sin terminar.");
        break;
      }
      await new Promise(resolve => setTimeout(resolve, POLL_MS));
      await pollPending(studentToken);
    }

    clearInterval(ticker);
    run.finishedAt = performance.now();
    render();
    const peak = peakConcurrency(run.items, run.finishedAt);
    log(`Prueba terminada en ${seconds(run.finishedAt - run.startedAt)}. Pico de evaluaciones simultáneas: ${peak}.`);
    setStatus("run-status", "online", "Terminada");
  } catch (e) {
    log(`La prueba falló: ${e.message}`);
    setStatus("run-status", "error", "Error");
  } finally {
    $("btn-run").disabled = false;
  }
}

// ─── Inicio ──────────────────────────────────────────────────────────────────

$("btn-health").addEventListener("click", checkHealth);
$("btn-run").addEventListener("click", launch);
$("workload").addEventListener("change", () => {
  const mix = $("workload").value === "mix";
  $("language").disabled = mix;
  if (mix) $("language").value = "PYTHON";
});
$("expected-workers").addEventListener("input", () => {
  $("t-peak-note").textContent = `de ${Math.max(1, Number($("expected-workers").value) || 1)} workers esperados`;
});

loadConfig();
checkHealth();
