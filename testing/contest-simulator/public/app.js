"use strict";

// ─── Utilidades ──────────────────────────────────────────────────────────────

const $ = id => document.getElementById(id);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const sleep = ms => new Promise(r => setTimeout(r, ms));
const rand = (min, max) => min + Math.random() * (max - min);
const pick = list => list[Math.floor(Math.random() * list.length)];
const num = id => Number($(id).value);
const time = iso => new Date(iso).toLocaleTimeString("es-CO");

const OUTCOMES = ["ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "COMPILATION_ERROR"];
const SHORT = {
  ACCEPTED: "AC", WRONG_ANSWER: "WA", TIME_LIMIT_EXCEEDED: "TLE", MEMORY_LIMIT_EXCEEDED: "MLE",
  RUNTIME_ERROR: "RTE", COMPILATION_ERROR: "CE", JUDGE_ERROR: "JE", PENDING: "…",
};
// Igual que el backend: estos rechazos suman penalización si el problema se resuelve después.
const PENALIZED = new Set(["WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "MEMORY_LIMIT_EXCEEDED", "RUNTIME_ERROR"]);
// Un color de globo distinto por problema, repartiendo el tono en la rueda de color.
const balloon = i => {
  const h = (i * 137.5) % 360, f = n => {
    const k = (n + h / 30) % 12;
    return Math.round(255 * (0.55 - 0.45 * Math.max(-1, Math.min(k - 3, 9 - k, 1)))).toString(16).padStart(2, "0");
  };
  return `#${f(0)}${f(8)}${f(4)}`.toUpperCase();
};
const MIN_PROBLEMS = 10, MIN_TEAMS = 20;

// El problema k pide imprimir a*k + b; cada código produce a propósito un veredicto en el juez real.
const CODE = {
  ACCEPTED:            k => `a, b = map(int, input().split())\nprint(a * ${k} + b)`,
  WRONG_ANSWER:        k => `a, b = map(int, input().split())\nprint(a * ${k} + b + 1)`,
  TIME_LIMIT_EXCEEDED: () => `while True:\n    pass`,
  RUNTIME_ERROR:       () => `a, b = map(int, input().split())\nprint(a // 0)`,
  COMPILATION_ERROR:   () => `def solve(:\n    pass`,
};
// El backend rechaza un envío idéntico a otro del mismo equipo y problema.
const tagged = code => `${code}\n# intento ${crypto.randomUUID()}\n`;

const state = {
  cfg: null,
  org: null,
  contest: null,     // { id, start, end }
  problems: [],      // { alias, k, catalogId, contestProblemId }
  teams: [],         // { id, name, user, skill, solved: Set, current }
  submissions: [],   // { id, teamId, alias, intended, verdict, submittedAt }
  rejected: 0,
  running: false,
  board: [],         // scoreboard recibido por SSE
  source: null,
  flash: new Set(),
};

// ─── JWT (HS256 en el navegador) ─────────────────────────────────────────────

function b64url(input) {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let bin = "";
  bytes.forEach(b => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function signJwt(user) {
  const now = Math.floor(Date.now() / 1000);
  const payload = { sub: user.id, role: user.role, iss: state.cfg.jwtIssuer, iat: now, exp: now + 4 * 3600 };
  const input = `${b64url(JSON.stringify({ alg: "HS256", typ: "JWT" }))}.${b64url(JSON.stringify(payload))}`;
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(state.cfg.jwtSecret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return `${input}.${b64url(new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(input))))}`;
}

// ─── Registro y API ──────────────────────────────────────────────────────────

function log(text, kind = "info") {
  const div = document.createElement("div");
  div.className = `entry ${kind}`;
  div.textContent = `[${new Date().toLocaleTimeString("es-CO")}] ${text}`;
  $("log").prepend(div);
}

async function api(method, path, body, as) {
  const res = await fetch(`/api/v1${path}`, {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${await signJwt(as)}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) log(`${method} ${path} ⇒ ${res.status} ${data?.message ?? text}`, "err");
  return { ok: res.ok, status: res.status, data };
}

const setStatus = text => ($("status").textContent = text);

// ─── Preparación ─────────────────────────────────────────────────────────────

async function prepare() {
  if (!state.cfg.jwtSecret) { log("Falta JWT_SECRET: arranca con start.sh para que se lea del .env.", "err"); return; }
  if (num("p-problems") < MIN_PROBLEMS || num("p-teams") < MIN_TEAMS) {
    log(`La simulación necesita al menos ${MIN_PROBLEMS} problemas y ${MIN_TEAMS} equipos.`, "err");
    return;
  }
  stop();
  $("btn-prepare").disabled = true;
  try {
    const tag = Math.random().toString(36).slice(2, 6);
    const org = { id: crypto.randomUUID(), role: "ORGANIZER" };
    const problems = [];

    for (let i = 0; i < num("p-problems"); i++) {
      const alias = String.fromCharCode(65 + i), k = i + 2;
      setStatus(`Creando problema ${alias}…`);
      const p = await api("POST", "/problems", {
        createdBy: org.id, title: `Simulación ${tag} ${alias}`,
        statement: `Lee dos enteros a y b e imprime a*${k} + b.`,
        timeLimitMs: 1000, memoryLimitKb: 65536, difficult: 800 + 100 * i, inputFormat: "a b", outputFormat: `a*${k} + b`,
      }, org);
      if (!p.ok) return;
      const cases = [[1, 2], [5, 7], [100, -3]].map(([a, b], j) => ({
        orderIndex: j + 1, input: `${a} ${b}\n`, output: `${a * k + b}\n`, expectedOutput: `${a * k + b}\n`, isSample: j === 0,
      }));
      if (!(await api("POST", `/problems/test-cases/${p.data.id}/batch`, { testCases: cases }, org)).ok) return;
      problems.push({ alias, k, catalogId: p.data.id });
    }

    setStatus("Creando concurso…");
    const start = new Date(Date.now() + num("p-lead") * 1000);
    const end = new Date(start.getTime() + num("p-duration") * 60_000);
    const c = await api("POST", "/contests", {
      createdBy: org.id, title: `Simulación ${tag}`, description: "Creado por el simulador de concurso",
      password: "sim", startTime: start.toISOString(), endTime: end.toISOString(),
    }, org);
    if (!c.ok) return;

    for (const [i, p] of problems.entries()) {
      const cp = await api("POST", `/contests/${c.data.id}/problems`, {
        problemId: p.catalogId, alias: p.alias, orderIndex: i,
        points: num("p-points"), penalty: num("p-penalty"), balloonColor: balloon(i),
      }, org);
      if (!cp.ok) return;
      p.contestProblemId = cp.data.id;
    }

    const teams = [];
    for (let i = 0; i < num("p-teams"); i++) {
      setStatus(`Inscribiendo equipo ${i + 1}…`);
      const user = { id: crypto.randomUUID(), role: "STUDENT" };
      const t = await api("POST", `/contests/${c.data.id}/teams`, { teamName: `Equipo ${tag}-${i + 1}`, contestPassword: "sim" }, user);
      if (!t.ok) return;
      teams.push({ id: t.data.id, name: t.data.teamName, user, skill: rand(0.5, 1.5), solved: new Set(), current: null });
    }

    Object.assign(state, {
      org, problems, teams, submissions: [], rejected: 0, board: [],
      contest: { id: c.data.id, start: new Date(c.data.startTime), end: new Date(c.data.endTime) },
    });
    connectScoreboard();
    render();
    log(`Concurso "${c.data.title}" listo: ${problems.length} problemas y ${teams.length} equipos. Empieza a las ${time(c.data.startTime)}.`, "ok");
    setStatus(`Listo. El concurso empieza a las ${time(c.data.startTime)}; pulsa "Iniciar simulación".`);
    $("btn-start").disabled = false;
  } finally {
    $("btn-prepare").disabled = false;
  }
}

// ─── Simulación ──────────────────────────────────────────────────────────────

async function start() {
  if (!state.contest) return;
  state.running = true;
  $("btn-start").disabled = true;
  $("btn-stop").disabled = false;

  while (state.running && Date.now() < state.contest.start.getTime() + 2000) {
    setStatus(`Esperando el inicio: faltan ${Math.ceil((state.contest.start - Date.now()) / 1000)} s`);
    await sleep(1000);
  }
  // El estado ACTIVE lo pone la tarea programada del backend al llegar la hora de inicio.
  for (let i = 0; state.running && i < 30; i++) {
    const c = await api("GET", `/contests/${state.contest.id}`, undefined, state.org);
    if (c.data?.status === "ACTIVE") break;
    setStatus(`Esperando que el backend marque el concurso como activo (${c.data?.status ?? "?"})…`);
    await sleep(1000);
  }
  if (!state.running) return;

  log("Simulación iniciada.", "ok");
  const ticker = setInterval(() => {
    const left = Math.max(0, Math.ceil((state.contest.end - Date.now()) / 1000));
    setStatus(state.running ? `En curso: quedan ${Math.floor(left / 60)}:${String(left % 60).padStart(2, "0")}` : "Detenida.");
  }, 1000);
  await Promise.all(state.teams.map(teamLoop));
  clearInterval(ticker);
  state.running = false;
  $("btn-stop").disabled = true;
  setStatus(Date.now() >= state.contest.end.getTime() ? "Concurso terminado." : "Detenida.");
  log("Simulación terminada.", "ok");
}

function stop() {
  state.running = false;
  $("btn-stop").disabled = true;
}

function pickOutcome(skill) {
  const weights = OUTCOMES.map(o => Math.max(0, num(`w-${o}`)) * (o === "ACCEPTED" ? skill : 1));
  let r = Math.random() * weights.reduce((a, b) => a + b, 0);
  return OUTCOMES.find((o, i) => (r -= weights[i]) < 0) ?? "WRONG_ANSWER";
}

async function teamLoop(team) {
  while (state.running && Date.now() < state.contest.end.getTime()) {
    await sleep(rand(num("p-think-min"), Math.max(num("p-think-min"), num("p-think-max"))) * 1000);
    if (!state.running || Date.now() >= state.contest.end.getTime()) break;

    const unsolved = state.problems.filter(p => !team.solved.has(p.alias));
    let problem, outcome;
    if (team.solved.size && (!unsolved.length || Math.random() * 100 < num("p-resend"))) {
      // Reenvío de un problema ya resuelto: no debe cambiar el scoreboard.
      problem = state.problems.find(p => p.alias === pick([...team.solved]));
      outcome = pick(OUTCOMES);
    } else if (unsolved.length) {
      if (!unsolved.includes(team.current)) team.current = pick(unsolved);
      problem = team.current;
      outcome = pickOutcome(team.skill);
    } else {
      continue;
    }

    const res = await api("POST", "/submissions",
      { teamId: team.id, problemId: problem.contestProblemId, language: "PYTHON", sourceCode: tagged(CODE[outcome](problem.k)) }, team.user);
    if (!res.ok) { state.rejected++; render(); continue; }

    const sub = { id: res.data.id, teamId: team.id, alias: problem.alias, intended: outcome, verdict: "PENDING", submittedAt: res.data.submittedAt };
    state.submissions.push(sub);
    render();
    // Como un concursante real, espera el veredicto antes de decidir qué hacer.
    if ((await waitVerdict(sub, team.user)) === "ACCEPTED") team.solved.add(problem.alias);
  }
}

async function waitVerdict(sub, user) {
  for (let i = 0; i < 120 && state.running; i++) {
    await sleep(1500);
    if (i === 13 && !state.warnedJudge) {
      state.warnedJudge = true;
      log("Hay envíos sin veredicto tras 20 s: revisa que goslint-judge esté corriendo y consumiendo submission.evaluate.", "err");
    }
    const r = await api("GET", `/submissions/${sub.id}`, undefined, user);
    if (r.ok && r.data.verdict !== "PENDING") {
      state.warnedJudge = false;
      sub.verdict = r.data.verdict;
      render();
      return sub.verdict;
    }
  }
  if (state.running) log(`El envío ${sub.id.slice(0, 8)} sigue sin veredicto tras 3 min; ¿está corriendo el juez?`, "err");
  return "PENDING";
}

// ─── Scoreboard ──────────────────────────────────────────────────────────────

function connectScoreboard() {
  state.source?.close();
  const es = new EventSource(`/api/v1/contests/${state.contest.id}/scoreboard/stream`);
  state.source = es;
  const badge = (text, on) => { $("sse-status").textContent = text; $("sse-status").classList.toggle("on", on); };
  es.onopen = () => badge("SSE conectado", true);
  es.onerror = () => badge("SSE reconectando…", false);
  es.addEventListener("scoreboard", e => {
    state.board = JSON.parse(e.data);
    log(`SSE: tabla completa (${state.board.length} equipos).`);
    render();
  });
  es.addEventListener("team", e => {
    const { position, team } = JSON.parse(e.data);
    state.board = state.board.filter(t => t.id !== team.id);
    state.board.splice(position - 1, 0, team);
    state.flash.add(team.id);
    log(`SSE: ${team.teamName} pasa a la posición ${position} (${team.score} pts, ${team.penaltyTime} min).`, "ok");
    render();
  });
}

// Mismas reglas que TeamStanding del backend, con los veredictos que devolvió el juez.
function expectedBoard() {
  return state.teams.map(team => {
    const subs = state.submissions.filter(s => s.teamId === team.id && s.verdict !== "PENDING")
      .sort((a, b) => new Date(a.submittedAt) - new Date(b.submittedAt));
    const solved = new Set(), fails = {};
    let score = 0, penalty = 0, last = null;
    for (const s of subs) {
      if (solved.has(s.alias)) continue;
      if (s.verdict === "ACCEPTED") {
        solved.add(s.alias);
        score += num("p-points");
        penalty += Math.floor((new Date(s.submittedAt) - state.contest.start) / 60_000) + num("p-penalty") * (fails[s.alias] ?? 0);
        last = new Date(s.submittedAt);
      } else if (PENALIZED.has(s.verdict)) {
        fails[s.alias] = (fails[s.alias] ?? 0) + 1;
      }
    }
    return { id: team.id, name: team.name, score, penalty, last, solved: [...solved].sort().join(" ") || "—" };
  }).sort((a, b) => b.score - a.score || a.penalty - b.penalty || (a.last ?? Infinity) - (b.last ?? Infinity));
}

// ─── Render ──────────────────────────────────────────────────────────────────

function render() {
  const counts = {};
  state.submissions.forEach(s => (counts[s.verdict] = (counts[s.verdict] ?? 0) + 1));
  const judged = state.submissions.filter(s => s.verdict !== "PENDING");
  const unexpected = judged.filter(s => s.verdict !== s.intended).length;
  $("stats").innerHTML = [
    ["Enviados", state.submissions.length],
    ["Rechazados por el backend", state.rejected],
    ...Object.entries(counts).map(([v, n]) => [SHORT[v] ?? v, n]),
    ["Veredicto distinto al buscado", unexpected],
  ].map(([k, v]) => `<span class="stat">${esc(k)}: <b>${v}</b></span>`).join("");

  const teamName = id => state.teams.find(t => t.id === id)?.name ?? id.slice(0, 8);
  $("submissions").innerHTML = state.submissions.slice(-40).reverse().map(s => `<tr>
      <td>${time(s.submittedAt)}</td><td>${esc(teamName(s.teamId))}</td><td>${s.alias}</td>
      <td class="v-${s.intended}">${SHORT[s.intended]}</td><td class="v-${s.verdict}">${SHORT[s.verdict] ?? s.verdict}</td>
    </tr>`).join("");

  const expected = expectedBoard();
  const backendPos = Object.fromEntries(state.board.map((t, i) => [t.id, { ...t, position: i }]));
  $("board-backend").innerHTML = state.board.map((t, i) => `<tr class="${state.flash.has(t.id) ? "flash" : ""}">
      <td>${i + 1}</td><td>${esc(t.teamName)}</td><td class="num">${t.score}</td><td class="num">${t.penaltyTime}</td>
    </tr>`).join("");
  state.flash.clear();
  $("board-expected").innerHTML = expected.map((t, i) => {
    const b = backendPos[t.id];
    const mismatch = !b || b.score !== t.score || b.penaltyTime !== t.penalty || b.position !== i;
    return `<tr class="${mismatch ? "mismatch" : ""}">
      <td>${i + 1}</td><td>${esc(t.name)}</td><td class="num">${t.score}</td><td class="num">${t.penalty}</td><td>${t.solved}</td>
    </tr>`;
  }).join("");
}

// ─── Inicio ──────────────────────────────────────────────────────────────────

$("btn-prepare").onclick = prepare;
$("btn-start").onclick = start;
$("btn-stop").onclick = stop;

(async () => {
  state.cfg = await (await fetch("/config")).json();
  log(state.cfg.jwtSecret ? `Backend: ${state.cfg.backendUrl}` : "No hay JWT_SECRET: arranca con start.sh.", state.cfg.jwtSecret ? "info" : "err");
})();
