/**
 * Rutas de maratones del contest-service en el mock: listado, detalle, problemas,
 * equipos de la maratón, ranking, inscripción por contraseña y aclaraciones.
 *
 * Códigos y mensajes siguen ContestExceptionHandler / TeamExceptionHandler:
 * 404 no existe, 409 conflicto de estado, 400 validación, 403 contraseña incorrecta.
 */
import type { ContestProblemDTO } from "@/features/contests/types/contest.types"
import { digest, isoAt, newId } from "./util"
import { type ClarificationRecord, type TeamRecord } from "./model"
import {
  contestStatusAt,
  findContest,
  scoreboardOf,
  toContestDTO,
  toTeamDTO,
  teamOfUserInContest,
} from "./domain"
import { ApiRouter, created, fieldErrors, httpError, ok, readBody, textField, type MockContext, type MockRequest } from "./router"
import { authenticate } from "./session"

const TEAM_NAME_MIN = 3
const TEAM_NAME_MAX = 50
const QUESTION_MAX = 1000

/** Validación de nombre de equipo (TeamName.of del backend). */
export function validateTeamName(raw: string): string {
  if (!raw) throw httpError(400, "El nombre del equipo no puede estar vacío.")
  if (raw.length < TEAM_NAME_MIN || raw.length > TEAM_NAME_MAX) {
    throw httpError(400, `El nombre del equipo debe tener entre ${TEAM_NAME_MIN} y ${TEAM_NAME_MAX} caracteres.`)
  }
  return raw
}

/** Mensaje de cierre según el estado de la maratón, como Contest.assertRegistrationOpen. */
function registrationClosedMessage(status: string): string {
  return status === "ENDED"
    ? "El concurso ya terminó: las inscripciones están cerradas."
    : "El concurso ya inició: las inscripciones están cerradas."
}

function listContests(_req: MockRequest, ctx: MockContext) {
  const contests = [...ctx.state.contests].sort((a, b) => Date.parse(a.startTime) - Date.parse(b.startTime))
  return ok(contests.map((c) => toContestDTO(c, ctx.now)))
}

function contestProblems(req: MockRequest, ctx: MockContext) {
  const contest = findContest(ctx.state, req.params.id)
  const list: ContestProblemDTO[] = ctx.state.contestProblems
    .filter((p) => p.contestId === contest.id)
    .sort((a, b) => a.orderIndex - b.orderIndex)
    .map((p) => ({ ...p }))
  return ok(list)
}

function joinContest(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const user = authenticate(state, req.authorization, now)
  const contest = findContest(state, req.params.id)

  const status = contestStatusAt(contest, now)
  if (status !== "UPCOMING") throw httpError(409, registrationClosedMessage(status))

  const body = readBody(req)
  const password = textField(body, "contestPassword") ?? ""
  if (!password || digest(password) !== contest.passwordDigest) {
    throw httpError(403, "La contraseña del concurso no es correcta.")
  }

  const teamName = validateTeamName((textField(body, "teamName") ?? "").trim())
  if (state.teams.some((t) => t.contestId === contest.id && t.teamName === teamName)) {
    throw httpError(409, `Ya existe un equipo llamado '${teamName}' en este concurso.`)
  }
  if (teamOfUserInContest(state, contest.id, user.id)) {
    throw httpError(409, "Ya eres integrante de un equipo en este concurso.")
  }

  const teamId = newId()
  const team: TeamRecord = {
    id: teamId,
    contestId: contest.id,
    captainId: user.id,
    teamName,
    createdAt: isoAt(now),
    members: [{ id: newId(), teamId, userId: user.id, registeredAt: isoAt(now) }],
  }
  state.teams.push(team)
  return created(toTeamDTO(state, team, now))
}

function toClarificationDTO(c: ClarificationRecord) {
  return {
    id: c.id,
    problemId: c.contestProblemId,
    teamId: c.teamId,
    question: c.question,
    answer: c.answer,
    status: c.status,
    createdAt: c.createdAt,
    answeredAt: c.answeredAt,
  }
}

function listClarifications(req: MockRequest, ctx: MockContext) {
  const contest = findContest(ctx.state, req.params.id)
  const contestProblemIds = new Set(
    ctx.state.contestProblems.filter((p) => p.contestId === contest.id).map((p) => p.id)
  )
  const list = ctx.state.clarifications
    .filter((c) => contestProblemIds.has(c.contestProblemId))
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
    .map(toClarificationDTO)
  return ok(list)
}

/** `POST /v1/contest-problems/{id}/clarifications`: la ruta usa el id de la relación concurso-problema. */
function askClarification(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const user = authenticate(state, req.authorization, now)
  const contestProblem = state.contestProblems.find((p) => p.id === req.params.id)
  if (!contestProblem) {
    throw httpError(404, `El problema del concurso con ID '${req.params.id}' no existe.`)
  }

  const body = readBody(req)
  const question = (textField(body, "question") ?? "").trim()
  if (!question) throw fieldErrors({ question: "La pregunta no puede estar vacía." })
  if (question.length > QUESTION_MAX) {
    throw fieldErrors({ question: `La pregunta no puede superar ${QUESTION_MAX} caracteres.` })
  }

  const team = teamOfUserInContest(state, contestProblem.contestId, user.id)
  if (!team) throw httpError(404, "No perteneces a ningún equipo de este concurso.")

  const record: ClarificationRecord = {
    id: newId(),
    contestProblemId: contestProblem.id,
    teamId: team.id,
    question,
    answer: null,
    status: "PENDING",
    createdAt: isoAt(now),
    answeredAt: null,
  }
  state.clarifications.push(record)
  return created(toClarificationDTO(record))
}

export function registerContestRoutes(router: ApiRouter): void {
  router.add("GET", "/v1/contests", listContests)
  router.add("GET", "/v1/contests/:id", (req, ctx) =>
    ok(toContestDTO(findContest(ctx.state, req.params.id), ctx.now))
  )
  router.add("GET", "/v1/contests/:id/problems", contestProblems)
  router.add("GET", "/v1/contests/:id/teams", (req, ctx) => {
    const contest = findContest(ctx.state, req.params.id)
    return ok(
      ctx.state.teams
        .filter((t) => t.contestId === contest.id)
        .map((t) => toTeamDTO(ctx.state, t, ctx.now))
    )
  })
  router.add("GET", "/v1/contests/:id/scoreboard", (req, ctx) => {
    const contest = findContest(ctx.state, req.params.id)
    return ok(scoreboardOf(ctx.state, contest.id, ctx.now))
  })
  router.add("POST", "/v1/contests/:id/teams", joinContest)
  router.add("GET", "/v1/contests/:id/clarifications", listClarifications)
  router.add("POST", "/v1/contest-problems/:id/clarifications", askClarification)
}
