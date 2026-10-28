"use strict";

// ─── Utilidades ──────────────────────────────────────────────────────────────

const $ = id => document.getElementById(id);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const short = id => (id ? String(id).slice(0, 8) : "—");
const fmtDate = iso => (iso ? new Date(iso).toLocaleString("es-CO") : "—");

// ─── JWT y enlaces firmados (HS256 en el navegador) ──────────────────────────

function b64url(input) {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let bin = "";
  bytes.forEach(b => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function hmac(secret, message) {
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return b64url(new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(message))));
}

async function signJwt(actor) {
  const now = Math.floor(Date.now() / 1000);
  const payload = { sub: actor.id, role: actor.role, iss: $("jwt-issuer").value, iat: now, exp: now + 3600 };
  const input = `${b64url(JSON.stringify({ alg: "HS256", typ: "JWT" }))}.${b64url(JSON.stringify(payload))}`;
  return `${input}.${await hmac($("jwt-secret").value, input)}`;
}

// Mismo token que InvitationLinks del backend: HMAC("team-invitation:<id>").
async function emailLink(invitationId, action) {
  const token = await hmac($("jwt-secret").value, `team-invitation:${invitationId}`);
  return `${state.contestUrl}/api/v1/invitations/${invitationId}/respond?action=${action}&token=${token}`;
}

// ─── Estado ──────────────────────────────────────────────────────────────────

const state = {
  contestUrl: "http://localhost:8086",
  problemUrl: "http://localhost:8082",
  submissionUrl: "http://localhost:8083",
  actors: [],
  actorId: null,
  contestId: null,
  contestTitle: null,
  teamId: null,
  teamName: null,
  teams: [],
  catalog: [],
  problems: [],
  clarifications: [],
  submissions: [],
};

const STORAGE_KEY = "teams-tester:actors";

function saveActors() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify({ actors: state.actors, actorId: state.actorId })); } catch { }
}

// Sesión: concurso y equipo elegidos y lo escrito en los formularios, para seguir donde se dejó.
const SESSION_KEY = "teams-tester:session";
const FORM_FIELDS = ["c-title", "c-password", "c-start", "c-duration", "t-name", "t-password", "t-filter",
  "k-question", "p-alias", "p-order", "p-points", "p-penalty", "p-color", "i-user", "i-email",
  "s-team-custom", "s-language", "s-code"];

function saveSession() {
  const fields = Object.fromEntries(FORM_FIELDS.map(id => [id, $(id).value]));
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify({
      contestId: state.contestId, contestTitle: state.contestTitle,
      teamId: state.teamId, teamName: state.teamName, fields,
    }));
  } catch { }
}

function loadSession() {
  try {
    const s = JSON.parse(localStorage.getItem(SESSION_KEY));
    if (!s) return;
    Object.assign(state, { contestId: s.contestId, contestTitle: s.contestTitle, teamId: s.teamId, teamName: s.teamName });
    for (const [id, v] of Object.entries(s.fields || {})) if ($(id) && v !== "") $(id).value = v;
  } catch { }
}

function newActor(name, role) {
  const slug = name.toLowerCase().split(" ")[0].normalize("NFD").replace(/[^a-z]/g, "");
  return { id: crypto.randomUUID(), name, role, email: `${slug}@example.com` };
}

function defaultActors() {
  return [
    newActor("Organizador", "ORGANIZER"),
    newActor("Ana (capitana)", "STUDENT"),
    newActor("Beto", "STUDENT"),
    newActor("Caro", "STUDENT"),
    newActor("Dani", "STUDENT"),
  ];
}

function loadActors() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved?.actors?.length) { state.actors = saved.actors; state.actorId = saved.actorId; return; }
  } catch { }
  state.actors = defaultActors();
}

const actor = () => state.actors.find(a => a.id === state.actorId) || state.actors[0];
const firstOf = role => state.actors.find(a => a.role === role);
const nameOf = userId => state.actors.find(a => a.id === userId)?.name || `usuario ${short(userId)}`;

// ─── Registro ────────────────────────────────────────────────────────────────

function log(text, kind = "info") {
  const div = document.createElement("div");
  div.className = `entry ${kind}`;
  div.textContent = `[${new Date().toLocaleTimeString("es-CO")}] ${text}`;
  $("log").prepend(div);
}

// ─── API ─────────────────────────────────────────────────────────────────────

// base: /svc → contest-service, /ssvc → submission-service.
async function api(method, path, body, as = actor(), base = "/svc") {
  if (!$("jwt-secret").value) { log("Falta JWT_SECRET en el panel 1.", "err"); return { ok: false, status: 0, data: null }; }
  const res = await fetch(`${base}/api/v1${path}`, {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${await signJwt(as)}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  const summary = data?.message
    ?? (Array.isArray(data) ? `${data.length} elemento(s)`
      : data?.id ? `${data.teamName || data.title || data.alias || data.status || ""} id=${short(data.id)}`.trim()
        : (data ? JSON.stringify(data) : ""));
  log(`${as.name} (${as.role}) → ${method} ${path}  ⇒  ${res.status}  ${summary}`, res.ok ? "ok" : "err");
  return { ok: res.ok, status: res.status, data };
}

async function checkHealth() {
  const dot = $("dot-contest");
  try {
    const r = await fetch("/svc/api/v1/contests");
    dot.className = `dot ${r.status === 502 ? "dot-down" : "dot-up"}`;
  } catch { dot.className = "dot dot-down"; }
  try {
    const r = await fetch("/psvc/api/v1/problems/all");
    $("dot-problem").className = `dot ${r.ok ? "dot-up" : "dot-down"}`;
  } catch { $("dot-problem").className = "dot dot-down"; }
  try {
    // Sin token responde 401/403: basta con que conteste para saber que está arriba.
    const r = await fetch("/ssvc/api/v1/submissions");
    $("dot-submission").className = `dot ${r.status === 502 ? "dot-down" : "dot-up"}`;
  } catch { $("dot-submission").className = "dot dot-down"; }
}

// ─── Usuarios de prueba ──────────────────────────────────────────────────────

function renderActors() {
  $("actors-table").querySelector("tbody").innerHTML = state.actors.map(a => `
    <tr data-id="${a.id}">
      <td><input data-field="name" value="${esc(a.name)}" aria-label="Nombre" /></td>
      <td><span class="pill">${a.role}</span></td>
      <td><input data-field="email" type="email" value="${esc(a.email)}" aria-label="Correo" /></td>
      <td class="mono">${a.id}</td>
      <td><button type="button" class="btn-small btn-danger" data-del="${a.id}">Quitar</button></td>
    </tr>`).join("");

  const options = state.actors.map(a => `<option value="${a.id}">${esc(a.name)} · ${a.role}</option>`).join("");
  $("actor").innerHTML = options;
  $("actor").value = actor().id;
  const invitee = $("i-user").value;
  $("i-user").innerHTML = state.actors.filter(a => a.role === "STUDENT").map(a => `<option value="${a.id}">${esc(a.name)}</option>`).join("");
  if (state.actors.some(a => a.id === invitee)) $("i-user").value = invitee;
  syncInviteEmail();
  renderActorBadge();
}

function renderActorBadge() {
  const a = actor();
  $("actor-badge").textContent = `${a.role} · ${short(a.id)}`;
  $("my-inv-who").textContent = `— de ${a.name}`;
}

function syncInviteEmail() {
  const a = state.actors.find(x => x.id === $("i-user").value);
  $("i-email").value = a ? a.email : "";
}

function setActor(id) {
  state.actorId = id;
  saveActors();
  $("actor").value = id;
  renderActorBadge();
  loadMyInvitations();
}

// ─── Concursos ───────────────────────────────────────────────────────────────

async function createContest(as, title, password, startMin, durationMin) {
  const start = new Date(Date.now() + startMin * 60_000);
  const end = new Date(start.getTime() + durationMin * 60_000);
  return api("POST", "/contests", {
    createdBy: as.id, title, description: "Creado desde el probador de equipos",
    password, startTime: start.toISOString(), endTime: end.toISOString(),
  }, as);
}

async function loadContests() {
  const { ok, data } = await api("GET", "/contests");
  if (!ok) return;
  const contests = [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  $("contests").innerHTML = contests.length ? contests.map(c => {
    const started = new Date(c.startTime) <= new Date();
    return `
      <div class="item ${c.id === state.contestId ? "selected" : ""}">
        <div class="grow"><strong>${esc(c.title)}</strong>
          <div class="muted">Inicia ${fmtDate(c.startTime)} · ${c.status}
            ${started ? '<span class="pill pill-EXPIRED">inscripción cerrada</span>' : '<span class="pill pill-ACCEPTED">inscripción abierta</span>'}</div>
        </div>
        <code>${short(c.id)}</code>
        <button type="button" class="btn-small" data-contest="${c.id}" data-title="${esc(c.title)}">Seleccionar</button>
      </div>`;
  }).join("") : '<p class="muted">No hay concursos.</p>';
}

function showSelection() {
  const c = state.contestTitle ? `— ${state.contestTitle}` : "— ninguno seleccionado";
  $("sel-contest").textContent = c;
  $("sel-contest-problems").textContent = c;
  $("sel-contest-clarifications").textContent = c;
  $("sel-contest-submissions").textContent = c;
  $("sel-team").textContent = state.teamName ? `— ${state.teamName}` : "— ningún equipo seleccionado";
}

function selectContest(id, title) {
  state.contestId = id;
  state.contestTitle = title;
  state.teamId = null;
  state.teamName = null;
  showSelection();
  saveSession();
  loadContests();
  loadTeams();
  loadProblems();
  loadClarifications();
}

// ─── Equipos ─────────────────────────────────────────────────────────────────

function requireContest() {
  if (!state.contestId) { log("Selecciona un concurso primero (panel 4).", "err"); return false; }
  return true;
}

function requireTeam() {
  if (!state.teamId) { log("Selecciona un equipo primero (panel 6).", "err"); return false; }
  return true;
}

function renderTeams(teams, ranked = false) {
  $("teams").innerHTML = teams.length ? teams.map((t, i) => `
    <div class="card ${t.id === state.teamId ? "selected" : ""}">
      <h4>${ranked ? `#${i + 1} · ` : ""}${esc(t.teamName)}</h4>
      <div class="muted">Capitán: ${esc(nameOf(t.captainId))} · ${t.members.length}/3 integrantes · ${t.score} pts, ${t.penaltyTime} pen.</div>
      <ul>${t.members.map(m => `
        <li>${esc(nameOf(m.userId))} ${m.userId === t.captainId ? '<span class="pill pill-captain">capitán</span>' : ""}
          <button type="button" class="btn-small btn-danger" data-remove="${t.id}" data-user="${m.userId}">Quitar</button></li>`).join("")}
      </ul>
      <div class="row">
        <button type="button" class="btn-small" data-team="${t.id}" data-name="${esc(t.teamName)}">Seleccionar</button>
        <button type="button" class="btn-small btn-ghost" data-rename="${t.id}">Renombrar</button>
        <button type="button" class="btn-small btn-danger" data-delete-team="${t.id}">Eliminar</button>
      </div>
    </div>`).join("") : '<p class="muted">Este concurso no tiene equipos.</p>';
}

async function loadTeams() {
  if (!state.contestId) return;
  const filter = $("t-filter").value.trim();
  const { ok, data } = await api("GET", `/contests/${state.contestId}/teams${filter ? `?name=${encodeURIComponent(filter)}` : ""}`);
  if (!ok) return;
  state.teams = data;
  if (state.teamId && !data.some(t => t.id === state.teamId)) { state.teamId = null; state.teamName = null; showSelection(); saveSession(); }
  renderTeams(data);
  renderClarifications();
  fillSubmissionSelectors();
}

function selectTeam(id, name) {
  state.teamId = id;
  state.teamName = name;
  showSelection();
  saveSession();
  renderTeams(state.teams);
  $("s-team").dataset.want = id;
  fillSubmissionSelectors();
}

// ─── Invitaciones ────────────────────────────────────────────────────────────

const teamName = teamId => state.teams.find(t => t.id === teamId)?.teamName || `equipo ${short(teamId)}`;

async function loadMyInvitations() {
  const { ok, data } = await api("GET", "/invitations/me");
  if (!ok) return;
  $("invitations").innerHTML = data.length ? data.map(i => `
    <div class="item">
      <div class="grow"><strong>${esc(teamName(i.teamId))}</strong> <span class="pill pill-${i.status}">${i.status}</span>
        <div class="muted">a ${esc(i.invitedEmail)} · enviada ${fmtDate(i.createdAt)}${i.respondedAt ? ` · respondida ${fmtDate(i.respondedAt)}` : ""}</div>
      </div>
      <button type="button" class="btn-small" data-accept="${i.id}">Aceptar (API)</button>
      <button type="button" class="btn-small btn-ghost" data-reject="${i.id}">Rechazar (API)</button>
      <button type="button" class="btn-small btn-ghost" data-link="${i.id}" data-action="accept">Enlace correo: aceptar ↗</button>
      <button type="button" class="btn-small btn-ghost" data-link="${i.id}" data-action="reject">Enlace correo: rechazar ↗</button>
    </div>`).join("") : '<p class="muted">Sin invitaciones para este usuario.</p>';
}

async function afterChange() {
  await loadTeams();
  await loadMyInvitations();
}

// ─── Problemas del concurso ──────────────────────────────────────────────────

// Colores de globo habituales en maratones, en el orden en que se sugieren.
const BALLOON_COLORS = ["#e53935", "#1e88e5", "#43a047", "#fdd835", "#fb8c00", "#8e24aa", "#ec407a", "#00acc1", "#6d4c41", "#757575"];

// Mismo SVG que el componente Balloon (JSX) del frontend: relleno = color, contorno = stroke.
// El contorno es claro porque el panel es oscuro y con #000 se perderían el brillo y el hilo.
const balloonSvg = (color, size = 26) => `
  <svg viewBox="0 0 100 172" width="${size}" height="${size * 1.72}" fill="none" stroke="#cfd3d6"
       stroke-width="5" stroke-linecap="round" stroke-linejoin="round" style="overflow:visible;flex:none" aria-hidden="true">
    <ellipse cx="50" cy="50" rx="44" ry="46" fill="${esc(color)}" />
    <path d="M78 50 A28 28 0 0 1 50 78" />
    <path d="M45 96 L39 109 L61 109 L55 96" fill="${esc(color)}" />
    <path d="M50 109 C38 124 62 138 50 152 C44 160 50 166 53 169" />
  </svg>`;

const problemTitle = problemId => state.catalog.find(p => p.id === problemId)?.title || `problema ${short(problemId)}`;

// Las operaciones de problemas exigen organizador: si el actor es estudiante se usa el primer organizador.
function staff() {
  const a = actor();
  if (a.role !== "STUDENT") return a;
  const org = firstOf("ORGANIZER");
  if (org) log(`${a.name} es estudiante: esta operación se hace como ${org.name}.`);
  return org || a;
}

async function loadCatalog() {
  try {
    const res = await fetch("/psvc/api/v1/problems/all");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    state.catalog = await res.json();
  } catch (err) {
    state.catalog = [];
    log(`No se pudo leer el catálogo de problem-service (${err.message}).`, "err");
  }
  const keep = $("p-problem").value;
  $("p-problem").innerHTML = state.catalog.length
    ? state.catalog.map(p => `<option value="${p.id}">${esc(p.title)} · ${short(p.id)}</option>`).join("")
    : '<option value="">(sin problemas en problem-service)</option>';
  if (state.catalog.some(p => p.id === keep)) $("p-problem").value = keep;
  renderProblems();
}

// Sugiere el siguiente alias libre (A, B, ... Z), la siguiente posición y el siguiente color.
function suggestProblemDefaults() {
  const used = new Set(state.problems.map(p => p.alias));
  const letter = [..."ABCDEFGHIJKLMNOPQRSTUVWXYZ"].find(l => !used.has(l)) || "";
  $("p-alias").value = letter;
  $("p-order").value = state.problems.length ? Math.max(...state.problems.map(p => p.orderIndex)) + 1 : 0;
  $("p-color").value = BALLOON_COLORS[state.problems.length % BALLOON_COLORS.length];
  const taken = new Set(state.problems.map(p => p.problemId));
  const free = state.catalog.find(p => !taken.has(p.id));
  if (free && taken.has($("p-problem").value)) $("p-problem").value = free.id;
}

function renderProblems() {
  $("problems").innerHTML = !state.contestId ? '<p class="muted">Selecciona un concurso (panel 4).</p>'
    : state.problems.length ? state.problems.map(p => `
    <div class="item problem-row" data-id="${p.id}">
      ${balloonSvg(p.balloonColor)}
      <div class="grow"><strong>${esc(p.alias)}</strong> · ${esc(problemTitle(p.problemId))}
        <div class="muted">${p.points} pt · ${p.penalty} min/intento fallido · posición ${p.orderIndex} · ${esc(p.balloonColor)} · <code>${short(p.id)}</code></div></div>
      <input type="text" data-f="alias" value="${esc(p.alias)}" maxlength="10" aria-label="Alias" />
      <input type="number" data-f="orderIndex" value="${p.orderIndex}" min="0" aria-label="Posición" />
      <input type="number" data-f="points" value="${p.points}" min="1" aria-label="Puntos" />
      <input type="number" data-f="penalty" value="${p.penalty}" min="0" aria-label="Penalización por intento fallido (min)" title="Penalización por intento fallido (min)" />
      <input type="color" data-f="balloonColor" value="${esc(p.balloonColor.toLowerCase())}" aria-label="Color del globo" />
      <button type="button" class="btn-small" data-save-problem="${p.id}">Guardar</button>
      <button type="button" class="btn-small btn-danger" data-remove-problem="${p.id}">Retirar</button>
    </div>`).join("") : '<p class="muted">Este concurso no tiene problemas.</p>';
  fillClarificationProblems();
  renderClarifications();
  fillSubmissionSelectors();
}

async function loadProblems() {
  if (!state.contestId) { state.problems = []; renderProblems(); return; }
  const { ok, data } = await api("GET", `/contests/${state.contestId}/problems`);
  if (!ok) return;
  state.problems = data;
  renderProblems();
  suggestProblemDefaults();
}

async function addProblem(as, body) {
  return api("POST", `/contests/${state.contestId}/problems`, body, as);
}

// ─── Aclaraciones ────────────────────────────────────────────────────────────

const teamLabel = teamId => state.teams.find(t => t.id === teamId)?.teamName || `equipo ${short(teamId)}`;
const problemAlias = id => state.problems.find(p => p.id === id)?.alias || short(id);

// El selector de problemas de la pregunta sigue a los problemas asignados al concurso (panel 5).
function fillClarificationProblems() {
  const keep = $("k-problem").value;
  $("k-problem").innerHTML = state.problems.length
    ? state.problems.map(p => `<option value="${p.id}">${esc(p.alias)} · ${esc(problemTitle(p.problemId))}</option>`).join("")
    : '<option value="">(el concurso no tiene problemas)</option>';
  if (state.problems.some(p => p.id === keep)) $("k-problem").value = keep;
}

function renderClarifications() {
  $("clarifications").innerHTML = !state.contestId ? '<p class="muted">Selecciona un concurso (panel 4).</p>'
    : state.clarifications.length ? state.clarifications.map(c => `
    <div class="item">
      <div class="grow">
        <strong>${esc(problemAlias(c.problemId))}</strong> · ${esc(teamLabel(c.teamId))}
        <span class="pill pill-${c.status}">${c.status}</span>
        <div class="muted">preguntó ${fmtDate(c.createdAt)}${c.answeredAt ? ` · respondida ${fmtDate(c.answeredAt)}` : ""}</div>
        <div>❓ ${esc(c.question)}</div>
        ${c.answer ? `<div>💬 ${esc(c.answer)}</div>` : ""}
      </div>
      ${c.status === "PENDING" ? `<div class="answer">
        <input type="text" placeholder="Respuesta…" maxlength="2000" aria-label="Respuesta" />
        <button type="button" class="btn-small" data-answer="${c.id}">Responder</button></div>` : ""}
    </div>`).join("") : '<p class="muted">Este concurso no tiene aclaraciones.</p>';
}

async function loadClarifications() {
  if (!state.contestId) { state.clarifications = []; renderClarifications(); return; }
  const { ok, data } = await api("GET", `/contests/${state.contestId}/clarifications`);
  if (!ok) return;
  state.clarifications = data;
  renderClarifications();
}

// ─── Envíos ──────────────────────────────────────────────────────────────────

// Equipos del concurso seleccionado + "otro UUID"; problemas del concurso primero y luego el resto del
// catálogo marcado "fuera del concurso", para poder provocar PROBLEM_NOT_IN_CONTEST a propósito.
function fillSubmissionSelectors() {
  const keepTeam = $("s-team").value, keepProblem = $("s-problem").value;
  $("s-team").innerHTML = state.teams.map(t => `<option value="${t.id}">${esc(t.teamName)} · ${short(t.id)}</option>`).join("")
    + '<option value="custom">Otro UUID (campo de al lado)</option>';
  // Un equipo recién seleccionado en el panel 6 manda, aunque aún no esté en la lista (loadTeams va detrás).
  const want = $("s-team").dataset.want;
  const pick = [want, keepTeam].find(id => id && [...$("s-team").options].some(o => o.value === id));
  if (pick) $("s-team").value = pick;
  if (pick && pick === want) delete $("s-team").dataset.want;

  const inContest = new Set(state.problems.map(p => p.problemId));
  $("s-problem").innerHTML = state.problems.map(p => `<option value="${p.problemId}">${esc(p.alias)} · ${esc(problemTitle(p.problemId))}</option>`).join("")
    + state.catalog.filter(p => !inContest.has(p.id))
      .map(p => `<option value="${p.id}">✗ ${esc(p.title)} (fuera del concurso)</option>`).join("");
  if ([...$("s-problem").options].some(o => o.value === keepProblem)) $("s-problem").value = keepProblem;
}

function submissionTarget() {
  const teamId = $("s-team").value === "custom" ? $("s-team-custom").value.trim() : $("s-team").value;
  return { teamId: teamId || null, problemId: $("s-problem").value || null };
}

async function submit(as, target, sourceCode, language = "PYTHON") {
  return api("POST", "/submissions", { ...target, language, sourceCode }, as, "/ssvc");
}

function renderSubmissions() {
  $("submissions").innerHTML = state.submissions.length ? state.submissions.map(s => `
    <div class="item">
      <div class="grow"><strong>${esc(problemTitle(s.problemId))}</strong> · ${s.language}
        <span class="pill pill-${s.status}">${s.status}</span> <span class="pill">${s.verdict}</span>
        <div class="muted">${fmtDate(s.submittedAt)} · equipo <code>${short(s.teamId)}</code></div>
      </div>
      <code>${short(s.id)}</code>
    </div>`).join("") : '<p class="muted">Sin envíos para este equipo.</p>';
}

async function loadSubmissions(teamId) {
  if (!teamId) { log("Elige un equipo en el panel 9.", "err"); return; }
  const { ok, data } = await api("GET", `/submissions/team/${teamId}`, undefined, actor(), "/ssvc");
  if (!ok) return;
  state.submissions = [...data].sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));
  renderSubmissions();
}

// ─── Pruebas automáticas de la regla de envíos ──────────────────────────────

// Texto que identifica cada motivo en el mensaje del 403 (SubmissionNotAllowedException).
const REASON_TEXT = {
  TEAM_NOT_FOUND: "equipo indicado no existe",
  NOT_A_MEMBER: "no es integrante",
  CONTEST_NOT_FOUND: "concurso no existe",
  CONTEST_NOT_ACTIVE: "no está activo",
  PROBLEM_NOT_IN_CONTEST: "no pertenece al concurso",
};

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function waitUntil(when, label) {
  while (Date.now() < when) {
    $("rule-tests-status").textContent = `${label}: faltan ${Math.ceil((when - Date.now()) / 1000)} s`;
    await sleep(Math.min(1000, when - Date.now()));
  }
}

// Devuelve el n-ésimo actor con ese rol, creándolo si no existe.
function ensureActor(role, index, name) {
  const all = state.actors.filter(a => a.role === role);
  if (all[index]) return all[index];
  const a = newActor(name, role);
  state.actors.push(a);
  saveActors();
  renderActors();
  log(`Las pruebas automáticas necesitaban otro ${role}: se creó "${name}".`);
  return a;
}

function ruleTestRow(n, label, as, expected) {
  const tr = document.createElement("tr");
  tr.innerHTML = `<td>…</td><td>${n}. ${esc(label)}</td><td>${esc(as.name)} · ${as.role}</td>
    <td>${expected.status}${expected.reason ? ` ${expected.reason}` : ""}</td><td class="muted">pendiente</td>`;
  $("rule-tests-table").querySelector("tbody").append(tr);
  return tr;
}

async function runRuleTests() {
  const org = ensureActor("ORGANIZER", 0, "Organizador");
  const admin = ensureActor("ADMIN", 0, "Admin");
  const ana = ensureActor("STUDENT", 0, "Ana (capitana)");
  const beto = ensureActor("STUDENT", 1, "Beto");
  if (state.catalog.length < 2) { log("Las pruebas automáticas necesitan al menos 2 problemas en problem-service.", "err"); return; }

  $("btn-rule-tests").disabled = true;
  $("rule-tests-table").querySelector("tbody").innerHTML = "";
  const tag = Math.random().toString(36).slice(2, 6);
  const [pIn, pOut] = state.catalog;
  let passed = 0, total = 0;

  try {
    $("rule-tests-status").textContent = "preparando escenario…";
    const a = await createContest(org, `Prueba ${tag} (activa 1 min)`, "clave123", 1, 1);
    if (!a.ok) throw new Error("no se pudo crear el concurso");
    const A = a.data.id;
    if (!(await api("POST", `/contests/${A}/problems`, { problemId: pIn.id, alias: "A", orderIndex: 0, points: 1, penalty: 20, balloonColor: BALLOON_COLORS[0] }, org)).ok)
      throw new Error("no se pudo asignar el problema al concurso");
    const t = await api("POST", `/contests/${A}/teams`, { teamName: `Prueba ${tag}`, contestPassword: "clave123" }, ana);
    if (!t.ok) throw new Error("no se pudo crear el equipo");
    const T = t.data.id;
    // El admin entra al equipo para mostrar que el rol no cuenta: solo ser integrante.
    if (!(await api("POST", `/teams/${T}/members`, { userId: admin.id }, org)).ok)
      throw new Error("no se pudo agregar al admin al equipo");
    const start = new Date(a.data.startTime).getTime(), end = new Date(a.data.endTime).getTime();
    log(`Pruebas ${tag}: concurso ${short(A)} con "${pIn.title}", equipo ${short(T)} de ${ana.name} y ${admin.name}; "${pOut.title}" queda fuera.`, "ok");

    let n = 0;
    const code = () => `print(sum(map(int, input().split())))  # prueba ${tag} caso ${n}`;
    async function check(label, as, target, expected, sourceCode = code()) {
      n++; total++;
      const tr = ruleTestRow(n, label, as, expected);
      const r = await submit(as, target, sourceCode);
      const msg = r.data?.message || "";
      const ok = r.status === expected.status && (!expected.reason || msg.includes(REASON_TEXT[expected.reason]));
      if (ok) passed++;
      tr.cells[0].textContent = ok ? "✅" : "❌";
      tr.cells[4].className = ok ? "" : "err-text";
      tr.cells[4].textContent = `${r.status}${msg ? ` · ${msg}` : r.data?.id ? ` · envío ${short(r.data.id)}` : ""}`;
      return { sourceCode, r };
    }

    // Fase 1: el concurso todavía no empieza.
    $("rule-tests-status").textContent = "fase 1: antes del inicio";
    await check("Integrante antes del inicio", ana, { teamId: T, problemId: pIn.id }, { status: 403, reason: "CONTEST_NOT_ACTIVE" });
    await check("Equipo inexistente", ana, { teamId: crypto.randomUUID(), problemId: pIn.id }, { status: 403, reason: "TEAM_NOT_FOUND" });
    await check("Estudiante que no es del equipo", beto, { teamId: T, problemId: pIn.id }, { status: 403, reason: "NOT_A_MEMBER" });
    await check("Organizador que creó el concurso (sin exención)", org, { teamId: T, problemId: pIn.id }, { status: 403, reason: "NOT_A_MEMBER" });

    // Fase 2: el concurso está activo.
    await waitUntil(start + 3000, "esperando el inicio del concurso");
    $("rule-tests-status").textContent = "fase 2: concurso activo";
    const okCase = await check("Integrante con concurso activo", ana, { teamId: T, problemId: pIn.id }, { status: 201 });
    await check("Admin integrante del equipo", admin, { teamId: T, problemId: pIn.id }, { status: 201 });
    await check("Admin con un equipo inexistente", admin, { teamId: crypto.randomUUID(), problemId: pIn.id }, { status: 403, reason: "TEAM_NOT_FOUND" });
    await check("Problema fuera del concurso", ana, { teamId: T, problemId: pOut.id }, { status: 403, reason: "PROBLEM_NOT_IN_CONTEST" });
    await check("Organizador que creó el concurso, durante el concurso", org, { teamId: T, problemId: pIn.id }, { status: 403, reason: "NOT_A_MEMBER" });
    await check("Mismo código otra vez (duplicado)", ana, { teamId: T, problemId: pIn.id }, { status: 409 }, okCase.sourceCode);

    // Fase 3: el concurso terminó.
    await waitUntil(end + 3000, "esperando el fin del concurso");
    $("rule-tests-status").textContent = "fase 3: concurso terminado";
    await check("Integrante después del fin", ana, { teamId: T, problemId: pIn.id }, { status: 403, reason: "CONTEST_NOT_ACTIVE" });

    selectContest(A, a.data.title);
    selectTeam(T, t.data.teamName);
    await loadSubmissions(T);
  } catch (err) {
    log(`Pruebas automáticas interrumpidas: ${err.message}.`, "err");
  } finally {
    $("btn-rule-tests").disabled = false;
    $("rule-tests-status").textContent = total ? `${passed}/${total} casos como se esperaba` : "";
    if (total) log(`Pruebas automáticas: ${passed}/${total} casos como se esperaba.`, passed === total ? "ok" : "err");
  }
}

// ─── Escenarios ──────────────────────────────────────────────────────────────

async function scenario(startMin) {
  const org = firstOf("ORGANIZER"), cap = firstOf("STUDENT");
  if (!org || !cap) { log("El escenario necesita al menos un organizador y un estudiante.", "err"); return; }
  const tag = Math.random().toString(36).slice(2, 6);
  const c = await createContest(org, `Escenario ${tag} (inicia en ${startMin} min)`, "clave123", startMin, 120);
  if (!c.ok) return;
  selectContest(c.data.id, c.data.title);
  const t = await api("POST", `/contests/${c.data.id}/teams`, { teamName: `Equipo ${tag}`, contestPassword: "clave123" }, cap);
  if (!t.ok) return;
  await loadTeams();
  selectTeam(t.data.id, t.data.teamName);
  setActor(cap.id);
  log(`Escenario listo: ${cap.name} es capitán de "Equipo ${tag}". Ahora invita desde el panel 7.`, "ok");
}

async function scenarioWithProblems() {
  const org = firstOf("ORGANIZER");
  if (!org) { log("El escenario necesita un organizador.", "err"); return; }
  if (state.catalog.length < 3) { log("problem-service necesita al menos 3 problemas para este escenario.", "err"); return; }
  const tag = Math.random().toString(36).slice(2, 6);
  const c = await createContest(org, `Escenario ${tag} con problemas`, "clave123", 60, 120);
  if (!c.ok) return;
  state.contestId = c.data.id;
  for (const [i, p] of state.catalog.slice(0, 3).entries()) {
    await addProblem(org, { problemId: p.id, alias: "ABC"[i], orderIndex: i, points: 1, penalty: 20, balloonColor: BALLOON_COLORS[i] });
  }
  selectContest(c.data.id, c.data.title);
  log(`Escenario listo: concurso con 3 problemas y globos. Puedes crear un equipo en el panel 6.`, "ok");
}

// ─── Eventos ─────────────────────────────────────────────────────────────────

$("btn-health").onclick = checkHealth;
$("actor").onchange = e => setActor(e.target.value);
$("i-user").onchange = syncInviteEmail;
$("btn-clear-log").onclick = () => ($("log").innerHTML = "");

$("actors-table").addEventListener("input", e => {
  const id = e.target.closest("tr")?.dataset.id;
  const a = state.actors.find(x => x.id === id);
  if (!a || !e.target.dataset.field) return;
  a[e.target.dataset.field] = e.target.value;
  saveActors();
  if (e.target.dataset.field === "email" && $("i-user").value === id) syncInviteEmail();
});
$("actors-table").addEventListener("change", () => renderActors());
$("actors-table").addEventListener("click", e => {
  const id = e.target.dataset.del;
  if (!id) return;
  state.actors = state.actors.filter(a => a.id !== id);
  if (!state.actors.length) state.actors = defaultActors();
  saveActors();
  renderActors();
});
$("btn-add-student").onclick = () => { state.actors.push(newActor(`Estudiante ${state.actors.length + 1}`, "STUDENT")); saveActors(); renderActors(); };
$("btn-add-organizer").onclick = () => { state.actors.push(newActor(`Organizador ${state.actors.length + 1}`, "ORGANIZER")); saveActors(); renderActors(); };
$("btn-add-admin").onclick = () => { state.actors.push(newActor(`Admin ${state.actors.length + 1}`, "ADMIN")); saveActors(); renderActors(); };
$("btn-reset-actors").onclick = () => { state.actors = defaultActors(); state.actorId = null; saveActors(); renderActors(); };

$("btn-scn-basic").onclick = () => scenario(60);
$("btn-scn-soon").onclick = () => scenario(1);
$("btn-scn-problems").onclick = scenarioWithProblems;
$("btn-forget").onclick = () => {
  try { localStorage.removeItem(SESSION_KEY); } catch { }
  location.reload();
};

$("btn-reload-catalog").onclick = loadCatalog;
$("btn-list-problems").onclick = () => requireContest() && loadProblems();
$("btn-add-problem").onclick = async () => {
  if (!requireContest()) return;
  const r = await addProblem(staff(), {
    problemId: $("p-problem").value, alias: $("p-alias").value, orderIndex: Number($("p-order").value),
    points: Number($("p-points").value), penalty: Number($("p-penalty").value), balloonColor: $("p-color").value,
  });
  if (r.ok) await loadProblems();
};
$("problems").addEventListener("click", async e => {
  const d = e.target.dataset;
  if (d.saveProblem) {
    const row = e.target.closest(".problem-row");
    const val = f => row.querySelector(`[data-f="${f}"]`).value;
    const r = await api("PUT", `/contest-problems/${d.saveProblem}`, {
      alias: val("alias"), orderIndex: Number(val("orderIndex")), points: Number(val("points")),
      penalty: Number(val("penalty")), balloonColor: val("balloonColor"),
    }, staff());
    if (r.ok) loadProblems();
  }
  if (d.removeProblem && (await api("DELETE", `/contest-problems/${d.removeProblem}`, undefined, staff())).ok) loadProblems();
});

$("btn-list-clarifications").onclick = () => requireContest() && loadClarifications();
$("btn-ask").onclick = async () => {
  if (!requireContest()) return;
  const r = await api("POST", `/contest-problems/${$("k-problem").value}/clarifications`, { question: $("k-question").value });
  if (r.ok) { $("k-question").value = ""; saveSession(); await loadClarifications(); }
};
$("clarifications").addEventListener("click", async e => {
  const id = e.target.dataset.answer;
  if (!id) return;
  const text = e.target.closest(".answer").querySelector("input").value;
  if ((await api("POST", `/clarifications/${id}/answer`, { answer: text }, staff())).ok) loadClarifications();
});

$("btn-submit").onclick = async () => {
  const target = submissionTarget();
  let code = $("s-code").value;
  if ($("s-unique").checked) code += `\n# envio ${Date.now()}`;
  const r = await submit(actor(), target, code, $("s-language").value);
  if (r.ok && target.teamId) await loadSubmissions(target.teamId);
};
$("btn-list-submissions").onclick = () => loadSubmissions(submissionTarget().teamId);
$("btn-rule-tests").onclick = runRuleTests;

// Guarda la sesión al escribir en cualquier campo persistente.
document.addEventListener("input", e => { if (FORM_FIELDS.includes(e.target.id)) saveSession(); });
$("i-user").addEventListener("change", saveSession);
$("s-language").addEventListener("change", saveSession);

$("btn-create-contest").onclick = async () => {
  const r = await createContest(actor(), $("c-title").value, $("c-password").value, Number($("c-start").value), Number($("c-duration").value));
  if (r.ok) selectContest(r.data.id, r.data.title);
};
$("btn-list-contests").onclick = loadContests;
$("contests").addEventListener("click", e => {
  const b = e.target.closest("[data-contest]");
  if (b) selectContest(b.dataset.contest, b.dataset.title);
});

$("btn-create-team").onclick = async () => {
  if (!requireContest()) return;
  const r = await api("POST", `/contests/${state.contestId}/teams`, { teamName: $("t-name").value, contestPassword: $("t-password").value });
  if (r.ok) { await loadTeams(); selectTeam(r.data.id, r.data.teamName); }
};
$("btn-list-teams").onclick = () => requireContest() && loadTeams();
$("btn-scoreboard").onclick = async () => {
  if (!requireContest()) return;
  const { ok, data } = await api("GET", `/contests/${state.contestId}/scoreboard`);
  if (ok) renderTeams(data, true);
};
$("teams").addEventListener("click", async e => {
  const d = e.target.dataset;
  if (d.team) selectTeam(d.team, d.name);
  if (d.rename) {
    const name = prompt("Nuevo nombre del equipo:");
    if (name && (await api("PUT", `/teams/${d.rename}`, { teamName: name })).ok) loadTeams();
  }
  if (d.deleteTeam && confirm("¿Eliminar el equipo? Requiere ADMIN u ORGANIZER.")) {
    if ((await api("DELETE", `/teams/${d.deleteTeam}`)).ok) {
      if (state.teamId === d.deleteTeam) { state.teamId = null; state.teamName = null; showSelection(); saveSession(); }
      loadTeams();
    }
  }
  if (d.remove && (await api("DELETE", `/teams/${d.remove}/members/${d.user}`)).ok) loadTeams();
});

$("btn-invite").onclick = async () => {
  if (!requireTeam()) return;
  const r = await api("POST", `/teams/${state.teamId}/invitations`, { userId: $("i-user").value, email: $("i-email").value });
  if (r.ok) log(`Invitación enviada a ${$("i-email").value}. Cambia el actor a ${nameOf($("i-user").value)} para responderla.`, "ok");
};
$("btn-add-member").onclick = async () => {
  if (!requireTeam()) return;
  if ((await api("POST", `/teams/${state.teamId}/members`, { userId: $("i-user").value })).ok) loadTeams();
};
$("btn-my-invitations").onclick = loadMyInvitations;
$("invitations").addEventListener("click", async e => {
  const d = e.target.dataset;
  // También tras un error: aceptar con el equipo lleno cambia el estado a EXPIRED.
  if (d.accept) { await api("POST", `/invitations/${d.accept}/accept`); afterChange(); }
  if (d.reject) { await api("POST", `/invitations/${d.reject}/reject`); afterChange(); }
  if (d.link) {
    // Abre la misma página que el botón del correo; al volver, recarga para ver el cambio.
    window.open(await emailLink(d.link, d.action), "_blank", "noopener");
    log(`Abierto el enlace del correo (${d.action}). Confirma en la pestaña nueva y pulsa "Recargar".`);
  }
});

// ─── Init ────────────────────────────────────────────────────────────────────

async function loadConfig() {
  try {
    const cfg = await (await fetch("/api/config")).json();
    $("jwt-secret").value = cfg.jwtSecret;
    $("jwt-issuer").value = cfg.jwtIssuer;
    state.contestUrl = cfg.contestUrl;
    state.problemUrl = cfg.problemUrl;
    state.submissionUrl = cfg.submissionUrl;
    $("submission-url").textContent = cfg.submissionUrl.replace(/^https?:\/\//, "");
    $("problem-url").textContent = cfg.problemUrl.replace(/^https?:\/\//, "");
    $("contest-url").textContent = cfg.contestUrl.replace(/^https?:\/\//, "");
    log(cfg.jwtSecretPresent ? "Configuración cargada desde .env." : "Falta JWT_SECRET en el .env: pégalo en el panel 1.", cfg.jwtSecretPresent ? "ok" : "err");
  } catch (err) {
    log(`No se pudo cargar /api/config (${err.message}). Rellena el panel 1 a mano.`, "err");
  }
}

(async function init() {
  loadActors();
  renderActors();
  loadSession(); // después de renderActors: el selector de invitados ya tiene opciones
  if (state.teamId) $("s-team").dataset.want = state.teamId;
  showSelection();
  await loadConfig();
  checkHealth();
  await loadCatalog();
  loadContests();
  await loadProblems();
  loadClarifications();
  loadTeams();
  loadMyInvitations();
})();
