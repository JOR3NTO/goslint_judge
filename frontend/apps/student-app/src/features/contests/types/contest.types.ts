/**
 * Tipos de la feature `contests`.
 *
 * Los `*DTO` mapean el contrato del contest-service y del problem-service
 * (rutas `/api/v1/contests/**`, `/api/v1/teams/**`, `/api/v1/problems/**`).
 * Los `*Item` / `*View` son modelos de vista que construyen los hooks.
 */

/** Estado de una maratón tal como lo devuelve el backend. */
export type ContestStatus = "UPCOMING" | "ACTIVE" | "ENDED"

/** `GET /v1/contests` y `GET /v1/contests/{id}`. Fechas en ISO-8601 (Instant). */
export interface ContestDTO {
  id: string
  createdBy: string
  title: string
  description: string
  startTime: string
  endTime: string
  status: ContestStatus
  createdAt: string
}

/** Problema asociado a una maratón (`GET /v1/contests/{id}/problems`). */
export interface ContestProblemDTO {
  id: string
  contestId: string
  problemId: string
  alias: string
  orderIndex: number
  points: number
  penalty: number
  /** Color del globo tal como lo guarda el backend (nombre CSS o hex). */
  balloonColor: string
}

/** Miembro de un equipo. */
export interface TeamMemberDTO {
  id: string
  teamId: string
  userId: string
  registeredAt: string
}

/** Equipo de una maratón (`/teams`, `/scoreboard`, `/teams/user/{userId}`). */
export interface TeamDTO {
  id: string
  contestId: string
  captainId: string
  teamName: string
  score: number
  penaltyTime: number
  createdAt: string
  members: TeamMemberDTO[]
}

/** Body de `POST /v1/contests/{id}/teams`. */
export interface JoinContestRequest {
  teamName: string
  contestPassword: string
}

/** Estado de una invitación (enum `InvitationStatus` del backend). */
export type InvitationStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED"

/** `POST /v1/teams/{teamId}/invitations` y elementos de `GET /v1/invitations/me`. */
export interface InvitationDTO {
  id: string
  teamId: string
  invitedUserId: string
  invitedEmail: string
  status: InvitationStatus
  createdAt: string
  respondedAt: string | null
}

/**
 * Body de `POST /v1/teams/{teamId}/invitations`.
 * Solo lleva `userId`: el backend toma el correo del usuario invitado.
 */
export interface InviteMemberRequest {
  userId: string
}

/** Elemento de `GET /v1/users/search?q=` — datos mínimos para elegir a quién invitar. */
export interface UserSummaryDTO {
  id: string
  username: string
}

/** Body de `PUT /v1/teams/{id}`. */
export interface UpdateTeamRequest {
  teamName: string
}

/** `GET /v1/problems/{problemId}` (problem-service). */
export interface ProblemDTO {
  id: string
  title: string
  statement: string
  timeLimitMs: number
  memoryLimitKb: number
  difficult: number
  inputFormat: string
  outputFormat: string
}

/** Forma del cuerpo de error del backend: `{ error: "..." }` o `{ campo: "mensaje" }`. */
export type ContestErrorBody = { error?: string } & Record<string, unknown>

/** Errores de validación por campo devueltos por el backend. */
export type ContestFieldErrors = Record<string, string>

/** Maratón lista para tarjetas: incluye la duración calculada en cliente. */
export interface ContestListItem extends ContestDTO {
  durationMs: number
}

/** Problema de la maratón enriquecido con su título (si pudo cargarse). */
export interface ContestProblemItem extends ContestProblemDTO {
  title: string | null
}
