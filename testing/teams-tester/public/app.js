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
  actors: [],
  actorId: null,
  contestId: null,
  contestTitle: null,
  teamId: null,
  teams: [],
};

const STORAGE_KEY = "teams-tester:actors";

function saveActors() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify({ actors: state.actors, actorId: state.actorId })); } catch { }
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

async function api(method, path, body, as = actor()) {
  if (!$("jwt-secret").value) { log("Falta JWT_SECRET en el panel 1.", "err"); return { ok: false, status: 0, data: null }; }
  const res = await fetch(`/svc/api/v1${path}`, {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${await signJwt(as)}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  const summary = data?.message
    ?? (Array.isArray(data) ? `${data.length} elemento(s)`
      : data?.id ? `${data.teamName || data.title || data.status || ""} id=${short(data.id)}`.trim()
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

function selectContest(id, title) {
  state.contestId = id;
  state.contestTitle = title;
  state.teamId = null;
  $("sel-contest").textContent = `— ${title}`;
  $("sel-team").textContent = "— ningún equipo seleccionado";
  loadContests();
  loadTeams();
}

// ─── Equipos ─────────────────────────────────────────────────────────────────

function requireContest() {
  if (!state.contestId) { log("Selecciona un concurso primero (panel 4).", "err"); return false; }
  return true;
}

function requireTeam() {
  if (!state.teamId) { log("Selecciona un equipo primero (panel 5).", "err"); return false; }
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
  renderTeams(data);
}

function selectTeam(id, name) {
  state.teamId = id;
  $("sel-team").textContent = `— ${name}`;
  renderTeams(state.teams);
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
  log(`Escenario listo: ${cap.name} es capitán de "Equipo ${tag}". Ahora invita desde el panel 6.`, "ok");
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
$("btn-reset-actors").onclick = () => { state.actors = defaultActors(); state.actorId = null; saveActors(); renderActors(); };

$("btn-scn-basic").onclick = () => scenario(60);
$("btn-scn-soon").onclick = () => scenario(1);

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
      if (state.teamId === d.deleteTeam) { state.teamId = null; $("sel-team").textContent = "— ningún equipo seleccionado"; }
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
    $("contest-url").textContent = cfg.contestUrl.replace(/^https?:\/\//, "");
    log(cfg.jwtSecretPresent ? "Configuración cargada desde .env." : "Falta JWT_SECRET en el .env: pégalo en el panel 1.", cfg.jwtSecretPresent ? "ok" : "err");
  } catch (err) {
    log(`No se pudo cargar /api/config (${err.message}). Rellena el panel 1 a mano.`, "err");
  }
}

(async function init() {
  loadActors();
  renderActors();
  await loadConfig();
  checkHealth();
  loadContests();
  loadMyInvitations();
})();
