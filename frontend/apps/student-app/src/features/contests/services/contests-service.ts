/**
 * ContestsService — adapter de la feature `contests` para el contest-service
 * y el problem-service.
 *
 * Sigue el patrón Adapter (frontend/ARCHITECTURE.md §3): depende solo de
 * `IApiAdapter` y lanza `ContestApiError` tipados cuando el backend responde con error.
 *
 * Contrato de errores (igual que AuthApiError): el backend devuelve
 *   - `{ error: "mensaje" }`  → `serverMessage`
 *   - `{ campo: "mensaje" }`  → `fieldErrors`
 */
import type { IApiAdapter, ApiResponse } from "@/core/api"
import type {
  ContestDTO,
  ContestErrorBody,
  ContestFieldErrors,
  ContestProblemDTO,
  InvitationDTO,
  InviteMemberRequest,
  JoinContestRequest,
  ProblemDTO,
  TeamDTO,
  UpdateTeamRequest,
  UserSummaryDTO,
} from "../types/contest.types"

/** Error tipado de la feature. `status` es el HTTP status del backend (0 si no hubo respuesta). */
export class ContestApiError extends Error {
  readonly status: number
  /** Errores por campo (cuerpo `{ campo: "msg" }`). */
  readonly fieldErrors?: ContestFieldErrors
  /** Mensaje genérico del backend (cuerpo `{ error: "msg" }`). */
  readonly serverMessage?: string

  constructor(status: number, fieldErrors?: ContestFieldErrors, serverMessage?: string) {
    super(serverMessage ?? "No se pudo completar la operación.")
    this.name = "ContestApiError"
    this.status = status
    this.fieldErrors = fieldErrors
    this.serverMessage = serverMessage
  }
}

/**
 * Extrae `{ error }`, `{ message }` (ErrorResponseDTO de los controladores Java) y/o
 * `{ campo: msg }` de un cuerpo de error del backend.
 */
function parseErrorBody(status: number, body: unknown): ContestApiError {
  if (!body || typeof body !== "object") {
    return new ContestApiError(status)
  }

  const record = body as ContestErrorBody
  const rawMessage = typeof record.error === "string" ? record.error : record.message
  const serverMessage = typeof rawMessage === "string" && rawMessage ? rawMessage : undefined

  const fieldErrors: ContestFieldErrors = {}
  for (const [key, value] of Object.entries(record)) {
    if (key !== "error" && key !== "message" && typeof value === "string") fieldErrors[key] = value
  }

  return new ContestApiError(
    status,
    Object.keys(fieldErrors).length > 0 ? fieldErrors : undefined,
    serverMessage
  )
}

export class ContestsService {
  constructor(private api: IApiAdapter) {}

  /** `GET /v1/contests` — listado público de maratones. */
  async list(): Promise<ContestDTO[]> {
    return this.unwrap(await this.api.get<ContestDTO[]>("/v1/contests"))
  }

  /** `GET /v1/contests/{id}` */
  async getById(id: string): Promise<ContestDTO> {
    return this.unwrap(await this.api.get<ContestDTO>(`/v1/contests/${id}`))
  }

  /** `GET /v1/contests/{id}/problems` — problemas de la maratón. */
  async getProblems(contestId: string): Promise<ContestProblemDTO[]> {
    return this.unwrap(await this.api.get<ContestProblemDTO[]>(`/v1/contests/${contestId}/problems`))
  }

  /** `GET /v1/contests/{id}/teams` — equipos inscritos. */
  async getTeams(contestId: string): Promise<TeamDTO[]> {
    return this.unwrap(await this.api.get<TeamDTO[]>(`/v1/contests/${contestId}/teams`))
  }

  /** `GET /v1/contests/{id}/scoreboard` — equipos ya ordenados por el backend. */
  async getScoreboard(contestId: string): Promise<TeamDTO[]> {
    return this.unwrap(await this.api.get<TeamDTO[]>(`/v1/contests/${contestId}/scoreboard`))
  }

  /**
   * `POST /v1/contests/{id}/teams` — inscribe un equipo. Requiere sesión
   * (el adapter adjunta el token).
   * @throws {ContestApiError} con los errores de validación o del servidor.
   */
  async joinContest(contestId: string, teamName: string, contestPassword: string): Promise<TeamDTO> {
    const body: JoinContestRequest = { teamName, contestPassword }
    return this.unwrap(await this.api.post<TeamDTO>(`/v1/contests/${contestId}/teams`, body))
  }

  /** `GET /v1/teams/user/{userId}` — equipos del usuario (en cualquier maratón). */
  async getMyTeams(userId: string): Promise<TeamDTO[]> {
    return this.unwrap(await this.api.get<TeamDTO[]>(`/v1/teams/user/${userId}`))
  }

  /** `GET /v1/teams/{id}` — equipo con sus integrantes. */
  async getTeam(teamId: string): Promise<TeamDTO> {
    return this.unwrap(await this.api.get<TeamDTO>(`/v1/teams/${teamId}`))
  }

  /**
   * `PUT /v1/teams/{id}` — renombra el equipo. El backend valida nombre único en la maratón.
   * Solo el capitán debería llamarlo (la UI lo restringe; el backend NO lo verifica).
   */
  async updateTeam(teamId: string, teamName: string): Promise<TeamDTO> {
    const body: UpdateTeamRequest = { teamName }
    return this.unwrap(await this.api.put<TeamDTO>(`/v1/teams/${teamId}`, body))
  }

  /**
   * `DELETE /v1/teams/{id}/members/{userId}` — quita a un integrante (204).
   * Lo usa el capitán para sacar a alguien o un integrante para abandonar.
   * Solo a nivel de UI se restringe quién puede hacerlo: el backend no verifica el autor.
   */
  async removeMember(teamId: string, userId: string): Promise<void> {
    this.unwrap(await this.api.delete<void>(`/v1/teams/${teamId}/members/${userId}`))
  }

  /**
   * `POST /v1/teams/{teamId}/invitations` — solo capitán, antes del inicio y con cupo (máx. 3).
   * Devuelve la invitación creada (PENDING). No hay endpoint para listar las invitaciones de un equipo.
   */
  async invite(teamId: string, input: InviteMemberRequest): Promise<InvitationDTO> {
    return this.unwrap(await this.api.post<InvitationDTO>(`/v1/teams/${teamId}/invitations`, input))
  }

  /** `GET /v1/users/search?q=` — hasta 20 usuarios activos cuyo username contiene `q` (sin incluir al propio). */
  async searchUsers(query: string): Promise<UserSummaryDTO[]> {
    return this.unwrap(await this.api.get<UserSummaryDTO[]>(`/v1/users/search?q=${encodeURIComponent(query)}`))
  }

  /** `GET /v1/invitations/me` — invitaciones recibidas por el usuario autenticado (todos los estados). */
  async getMyInvitations(): Promise<InvitationDTO[]> {
    return this.unwrap(await this.api.get<InvitationDTO[]>("/v1/invitations/me"))
  }

  /** `POST /v1/invitations/{id}/accept` — acepta y devuelve el equipo actualizado. */
  async acceptInvitation(invitationId: string): Promise<TeamDTO> {
    return this.unwrap(await this.api.post<TeamDTO>(`/v1/invitations/${invitationId}/accept`, undefined))
  }

  /** `POST /v1/invitations/{id}/reject` — rechaza (204). */
  async rejectInvitation(invitationId: string): Promise<void> {
    this.unwrap(await this.api.post<void>(`/v1/invitations/${invitationId}/reject`, undefined))
  }

  /** `GET /v1/problems/{problemId}` (problem-service). Vive aquí por ahora. */
  async getProblem(problemId: string): Promise<ProblemDTO> {
    return this.unwrap(await this.api.get<ProblemDTO>(`/v1/problems/${problemId}`))
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    if (!response.ok) {
      throw parseErrorBody(response.status, response.data)
    }
    return response.data
  }
}
