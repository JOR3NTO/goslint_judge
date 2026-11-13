/**
 * Rutas de equipos e invitaciones del contest-service en el mock.
 *
 * Reglas tomadas de InviteMemberUseCaseImpl, AcceptInvitationUseCaseImpl y
 * RejectInvitationUseCaseImpl: solo el capitán invita, máximo 3 integrantes, las
 * invitaciones y las inscripciones solo antes del inicio, y una invitación aceptada
 * con el equipo lleno pasa a EXPIRED.
 *
 * Igual que el backend, renombrar y quitar integrantes NO comprueban que quien llama
 * sea el capitán: la restricción es solo de la UI.
 */
import type { InvitationDTO } from "@/features/contests/types/contest.types"
import { isoAt, newId } from "./util"
import { TEAM_MAX_MEMBERS, type TeamRecord } from "./model"
import { contestStatusAt, findContest, findTeam, teamOfUserInContest, toTeamDTO } from "./domain"
import { ApiRouter, httpError, noContent, ok, readBody, textField, created, type MockContext, type MockRequest } from "./router"
import { authenticate } from "./session"
import { validateTeamName } from "./routes-contests"

function findInvitation(ctx: MockContext, id: string): InvitationDTO {
  const invitation = ctx.state.invitations.find((i) => i.id === id)
  if (!invitation) throw httpError(404, `La invitación con ID '${id}' no existe.`)
  return invitation
}

function renameTeam(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  authenticate(state, req.authorization, now)
  const team = findTeam(state, req.params.id)
  const body = readBody(req)
  const teamName = validateTeamName((textField(body, "teamName") ?? "").trim())

  const clash = state.teams.some(
    (t) => t.id !== team.id && t.contestId === team.contestId && t.teamName === teamName
  )
  if (clash) throw httpError(409, `Ya existe un equipo llamado '${teamName}' en este concurso.`)

  team.teamName = teamName
  return ok(toTeamDTO(state, team, now))
}

function removeMember(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  authenticate(state, req.authorization, now)
  const team = findTeam(state, req.params.teamId)
  const index = team.members.findIndex((m) => m.userId === req.params.userId)
  if (index === -1) throw httpError(404, "El usuario no es integrante de este equipo.")

  team.members.splice(index, 1)
  if (team.members.length === 0) {
    // Sin integrantes el equipo deja de existir.
    state.teams = state.teams.filter((t) => t.id !== team.id)
  } else if (team.captainId === req.params.userId) {
    // Si sale el capitán, el primer integrante restante pasa a serlo (decisión del mock).
    team.captainId = team.members[0].userId
  }
  return noContent()
}

function invite(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const requester = authenticate(state, req.authorization, now)
  const team: TeamRecord = findTeam(state, req.params.teamId)

  if (team.captainId !== requester.id) {
    throw httpError(403, "Solo el capitán del equipo puede invitar integrantes.")
  }
  const contest = findContest(state, team.contestId)
  const status = contestStatusAt(contest, now)
  if (status !== "UPCOMING") {
    throw httpError(409, "Las invitaciones solo se pueden enviar antes del inicio del concurso.")
  }

  const body = readBody(req)
  const userId = (textField(body, "userId") ?? "").trim()
  if (!userId) throw httpError(400, "El ID del usuario es obligatorio.")

  if (team.members.length >= TEAM_MAX_MEMBERS) {
    throw httpError(409, `El equipo ya tiene el máximo de ${TEAM_MAX_MEMBERS} integrantes.`)
  }
  const invited = state.users.find((u) => u.id === userId)
  if (!invited) {
    throw httpError(404, "No existe un usuario con ese ID.")
  }
  if (teamOfUserInContest(state, team.contestId, userId)) {
    throw httpError(409, "El usuario ya es integrante de un equipo en este concurso.")
  }
  const pending = state.invitations.some(
    (i) => i.teamId === team.id && i.invitedUserId === userId && i.status === "PENDING"
  )
  if (pending) {
    throw httpError(409, "El usuario ya tiene una invitación pendiente para este equipo.")
  }

  const invitation: InvitationDTO = {
    id: newId(),
    teamId: team.id,
    invitedUserId: userId,
    invitedEmail: invited.email,
    status: "PENDING",
    createdAt: isoAt(now),
    respondedAt: null,
  }
  state.invitations.push(invitation)
  return created(invitation)
}

function acceptInvitation(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const user = authenticate(state, req.authorization, now)
  const invitation = findInvitation(ctx, req.params.id)

  if (invitation.invitedUserId !== user.id) {
    throw httpError(403, "La invitación no está dirigida a este usuario.")
  }
  if (invitation.status !== "PENDING") {
    throw httpError(409, "La invitación ya fue respondida o ya no es válida.")
  }

  const team = findTeam(state, invitation.teamId)
  const contest = findContest(state, team.contestId)
  if (contestStatusAt(contest, now) !== "UPCOMING") {
    throw httpError(409, "Las inscripciones ya están cerradas: el concurso ya inició.")
  }

  if (team.members.length >= TEAM_MAX_MEMBERS) {
    // Igual que el backend: la invitación se invalida y el error queda registrado.
    invitation.status = "EXPIRED"
    invitation.respondedAt = isoAt(now)
    throw httpError(
      409,
      `La invitación ya no es válida: el equipo alcanzó el máximo de ${TEAM_MAX_MEMBERS} integrantes.`
    )
  }
  if (teamOfUserInContest(state, team.contestId, user.id)) {
    throw httpError(409, "Ya eres integrante de un equipo en este concurso.")
  }

  team.members.push({ id: newId(), teamId: team.id, userId: user.id, registeredAt: isoAt(now) })
  invitation.status = "ACCEPTED"
  invitation.respondedAt = isoAt(now)
  return ok(toTeamDTO(state, team, now))
}

function rejectInvitation(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const user = authenticate(state, req.authorization, now)
  const invitation = findInvitation(ctx, req.params.id)

  if (invitation.invitedUserId !== user.id) {
    throw httpError(403, "La invitación no está dirigida a este usuario.")
  }
  if (invitation.status !== "PENDING") {
    throw httpError(409, "La invitación ya fue respondida o ya no es válida.")
  }
  invitation.status = "REJECTED"
  invitation.respondedAt = isoAt(now)
  return noContent()
}

export function registerTeamRoutes(router: ApiRouter): void {
  router.add("GET", "/v1/teams/user/:userId", (req, ctx) => {
    const teams = ctx.state.teams
      .filter((t) => t.members.some((m) => m.userId === req.params.userId))
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .map((t) => toTeamDTO(ctx.state, t, ctx.now))
    return ok(teams)
  })
  router.add("GET", "/v1/teams/:id", (req, ctx) => ok(toTeamDTO(ctx.state, findTeam(ctx.state, req.params.id), ctx.now)))
  router.add("PUT", "/v1/teams/:id", renameTeam)
  router.add("DELETE", "/v1/teams/:teamId/members/:userId", removeMember)
  router.add("POST", "/v1/teams/:teamId/invitations", invite)
  router.add("GET", "/v1/invitations/me", (req, ctx) => {
    const user = authenticate(ctx.state, req.authorization, ctx.now)
    const list = ctx.state.invitations
      .filter((i) => i.invitedUserId === user.id)
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
    return ok(list.map((i) => ({ ...i })))
  })
  router.add("GET", "/v1/users/search", (req, ctx) => {
    const me = authenticate(ctx.state, req.authorization, ctx.now)
    const term = (req.query.get("q") ?? "").trim().toLowerCase()
    const users = ctx.state.users
      .filter((u) => u.isActive && u.id !== me.id && u.username.toLowerCase().includes(term))
      .sort((a, b) => a.username.localeCompare(b.username))
      .slice(0, 20)
      .map((u) => ({ id: u.id, username: u.username }))
    return ok(users)
  })
  router.add("POST", "/v1/invitations/:id/accept", acceptInvitation)
  router.add("POST", "/v1/invitations/:id/reject", rejectInvitation)
}
