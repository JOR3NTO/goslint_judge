/**
 * Rutas del submission-service en el mock.
 *
 * Reglas de SubmissionEligibilityPolicy y SubmitCodeUseCaseImpl: el envío lo hace un
 * integrante del equipo, la maratón debe estar ACTIVE y el problema debe pertenecer a
 * ella. Todos esos rechazos responden 403, y un envío idéntico responde 409.
 */
import type { SubmissionRecord } from "./model"
import { newId, isoAt } from "./util"
import { SUBMISSION_LANGUAGES } from "./judge"
import { contestStatusAt, findContest, toSubmissionDTO } from "./domain"
import { ApiRouter, created, httpError, ok, readBody, textField, type MockContext, type MockRequest } from "./router"
import { authenticate } from "./session"

const MAX_SOURCE_BYTES = 65_536

function createSubmission(req: MockRequest, ctx: MockContext) {
  const { state, now } = ctx
  const user = authenticate(state, req.authorization, now)

  const body = readBody(req)
  const teamId = (textField(body, "teamId") ?? "").trim()
  const problemId = (textField(body, "problemId") ?? "").trim()
  const language = textField(body, "language") ?? ""
  const sourceCode = textField(body, "sourceCode") ?? ""

  if (!teamId) throw httpError(400, "El identificador del equipo es obligatorio.")
  if (!problemId) throw httpError(400, "El identificador del problema es obligatorio.")
  if (!SUBMISSION_LANGUAGES.includes(language as (typeof SUBMISSION_LANGUAGES)[number])) {
    throw httpError(400, `El lenguaje '${language}' no está permitido. Usa C, CPP, JAVA o PYTHON.`)
  }
  if (!sourceCode.trim()) throw httpError(400, "El código fuente no puede estar vacío.")
  if (new TextEncoder().encode(sourceCode).length > MAX_SOURCE_BYTES) {
    throw httpError(400, "El código fuente supera el tamaño máximo permitido.")
  }

  // Elegibilidad (SubmissionEligibilityPolicy, en el mismo orden que el backend).
  const team = state.teams.find((t) => t.id === teamId)
  if (!team) throw httpError(403, "El equipo indicado no existe o no está inscrito en una maratón.")
  if (!team.members.some((m) => m.userId === user.id)) {
    throw httpError(403, "Solo los integrantes del equipo pueden enviar soluciones en su nombre.")
  }
  const contest = findContest(state, team.contestId)
  if (contestStatusAt(contest, now) !== "ACTIVE") {
    throw httpError(403, "El concurso no está activo: solo se aceptan envíos durante la maratón.")
  }
  const belongs = state.contestProblems.some((p) => p.contestId === contest.id && p.id === problemId)
  if (!belongs) throw httpError(403, "El problema no pertenece a la maratón de este equipo.")

  const duplicate = state.submissions.some(
    (s) =>
      s.teamId === teamId &&
      s.problemId === problemId &&
      s.language === language &&
      s.sourceCode === sourceCode
  )
  if (duplicate) {
    throw httpError(409, "Ya enviaste exactamente este mismo código para este problema con este lenguaje.")
  }

  const record: SubmissionRecord = {
    id: newId(),
    teamId,
    problemId,
    language: language as SubmissionRecord["language"],
    sourceCode,
    submittedAt: isoAt(now),
    verdictOverride: null,
  }
  state.submissions.push(record)
  return created(toSubmissionDTO(state, record, now))
}

export function registerSubmissionRoutes(router: ApiRouter): void {
  router.add("POST", "/v1/submissions", createSubmission)
  router.add("GET", "/v1/submissions/team/:teamId", (req, ctx) => {
    const list = ctx.state.submissions
      .filter((s) => s.teamId === req.params.teamId)
      .sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
      .map((s) => toSubmissionDTO(ctx.state, s, ctx.now))
    return ok(list)
  })
  router.add("GET", "/v1/submissions/:id", (req, ctx) => {
    const record = ctx.state.submissions.find((s) => s.id === req.params.id)
    if (!record) throw httpError(404, `El envío con ID '${req.params.id}' no existe.`)
    return ok(toSubmissionDTO(ctx.state, record, ctx.now))
  })
}
