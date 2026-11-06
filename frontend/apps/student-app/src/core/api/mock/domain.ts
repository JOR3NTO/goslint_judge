/**
 * Reglas de dominio del mock: estado de las maratones, DTO de respuesta, ranking ICPC
 * y búsquedas con error 404. Las rutas usan estas funciones para responder igual que
 * los servicios reales.
 */
import type { ContestDTO, ContestStatus, TeamDTO } from "@/features/contests/types/contest.types"
import type { SubmissionDTO } from "@/features/arena/types/arena.types"
import type { UserProfile } from "@/features/profile/types/profile.types"
import {
  ICPC_WRONG_ATTEMPT_PENALTY_MIN,
  type ContestRecord,
  type MockState,
  type SubmissionRecord,
  type TeamRecord,
  type UserRecord,
} from "./model"
import { COUNTABLE_WRONG_VERDICTS, resolveSubmission } from "./judge"
import { httpError } from "./router"

const MIN_MS = 60_000

// ─── Maratones ───────────────────────────────────────────────────────────────

/** Estado calculado a partir de la hora: UPCOMING antes del inicio, ACTIVE hasta el fin, luego ENDED. */
export function contestStatusAt(contest: Pick<ContestRecord, "startTime" | "endTime">, now: number): ContestStatus {
  if (now < Date.parse(contest.startTime)) return "UPCOMING"
  if (now < Date.parse(contest.endTime)) return "ACTIVE"
  return "ENDED"
}

export function toContestDTO(contest: ContestRecord, now: number): ContestDTO {
  return {
    id: contest.id,
    createdBy: contest.createdBy,
    title: contest.title,
    description: contest.description,
    startTime: contest.startTime,
    endTime: contest.endTime,
    status: contestStatusAt(contest, now),
    createdAt: contest.createdAt,
  }
}

export function findContest(state: MockState, contestId: string): ContestRecord {
  const contest = state.contests.find((c) => c.id === contestId)
  if (!contest) throw httpError(404, `El concurso con ID '${contestId}' no existe.`)
  return contest
}

// ─── Usuarios ────────────────────────────────────────────────────────────────

export function toUserProfile(user: UserRecord): UserProfile {
  return {
    id: user.id,
    firstName: user.firstName,
    lastName: user.lastName,
    username: user.username,
    email: user.email,
    role: user.role,
    institution: user.institution,
    isActive: user.isActive,
    createdAt: user.createdAt,
  }
}

// ─── Equipos y ranking ───────────────────────────────────────────────────────

export function findTeam(state: MockState, teamId: string): TeamRecord {
  const team = state.teams.find((t) => t.id === teamId)
  if (!team) throw httpError(404, `El equipo con ID '${teamId}' no existe.`)
  return team
}

export function teamOfUserInContest(state: MockState, contestId: string, userId: string): TeamRecord | undefined {
  return state.teams.find(
    (t) => t.contestId === contestId && t.members.some((m) => m.userId === userId)
  )
}

/**
 * Puntaje y penalización ICPC del equipo, calculados desde los envíos ya juzgados:
 * cada problema resuelto suma sus puntos y aporta los minutos hasta el primer AC más
 * la penalización por cada intento fallido anterior.
 */
export function teamScore(
  state: MockState,
  team: TeamRecord,
  now: number
): { score: number; penaltyTime: number } {
  const contest = state.contests.find((c) => c.id === team.contestId)
  if (!contest) return { score: 0, penaltyTime: 0 }
  const startMs = Date.parse(contest.startTime)

  let score = 0
  let penaltyTime = 0

  for (const cp of state.contestProblems.filter((p) => p.contestId === team.contestId)) {
    const problem = state.problems.find((p) => p.id === cp.problemId)
    const attempts = state.submissions
      .filter((s) => s.teamId === team.id && s.problemId === cp.problemId)
      .sort((a, b) => Date.parse(a.submittedAt) - Date.parse(b.submittedAt))

    let wrong = 0
    for (const attempt of attempts) {
      const resolved = resolveSubmission(attempt, now, problem)
      if (resolved.status !== "JUDGED") continue
      if (resolved.verdict === "ACCEPTED") {
        score += cp.points
        const minutes = Math.max(0, Math.floor((Date.parse(attempt.submittedAt) - startMs) / MIN_MS))
        penaltyTime += minutes + wrong * ICPC_WRONG_ATTEMPT_PENALTY_MIN
        break
      }
      if (COUNTABLE_WRONG_VERDICTS.has(resolved.verdict)) wrong++
    }
  }

  return { score, penaltyTime }
}

export function toTeamDTO(state: MockState, team: TeamRecord, now: number): TeamDTO {
  const { score, penaltyTime } = teamScore(state, team, now)
  return {
    id: team.id,
    contestId: team.contestId,
    captainId: team.captainId,
    teamName: team.teamName,
    score,
    penaltyTime,
    createdAt: team.createdAt,
    members: team.members.map((m) => ({ ...m })),
  }
}

/** Ranking de una maratón: más puntos primero, luego menos penalización y nombre. */
export function scoreboardOf(state: MockState, contestId: string, now: number): TeamDTO[] {
  return state.teams
    .filter((t) => t.contestId === contestId)
    .map((t) => toTeamDTO(state, t, now))
    .sort(
      (a, b) =>
        b.score - a.score || a.penaltyTime - b.penaltyTime || a.teamName.localeCompare(b.teamName, "es")
    )
}

// ─── Envíos ──────────────────────────────────────────────────────────────────

export function toSubmissionDTO(state: MockState, record: SubmissionRecord, now: number): SubmissionDTO {
  const problem = state.problems.find((p) => p.id === record.problemId)
  const resolved = resolveSubmission(record, now, problem)
  return {
    id: record.id,
    teamId: record.teamId,
    problemId: record.problemId,
    language: record.language,
    sourceCode: record.sourceCode,
    verdict: resolved.verdict,
    status: resolved.status,
    executionTimeMs: resolved.executionTimeMs,
    memoryUsedKb: resolved.memoryUsedKb,
    codeSizeBytes: new TextEncoder().encode(record.sourceCode).length,
    submittedAt: record.submittedAt,
  }
}
