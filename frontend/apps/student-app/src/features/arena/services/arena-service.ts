/**
 * ArenaService — adapter de la feature `arena` para el submission-service
 * (`/v1/submissions/**`) y las muestras del problem-service.
 *
 * Depende solo de `IApiAdapter` (frontend/ARCHITECTURE.md §3). Las rutas son
 * relativas: `HttpApiAdapter` antepone `/api` y next.config.mjs las reenvía.
 */
import { createHttpApi } from "@/core/api"
import type { ApiResponse, IApiAdapter } from "@/core/api"
import type {
  SampleDTO,
  SubmissionDTO,
  SubmitSolutionRequest,
} from "../types/arena.types"

/** Error tipado de la feature. `status` es el HTTP status (0 si no hubo respuesta). */
export class ArenaApiError extends Error {
  readonly status: number
  /** Mensaje del backend (cuerpo `{ error: "msg" }`), si existe. */
  readonly serverMessage?: string

  constructor(status: number, serverMessage?: string) {
    super(serverMessage ?? `No se pudo completar la operación (HTTP ${status}).`)
    this.name = "ArenaApiError"
    this.status = status
    this.serverMessage = serverMessage
  }
}

function toError(response: ApiResponse<unknown>): ArenaApiError {
  const body = response.data as { error?: unknown } | null
  const serverMessage = typeof body?.error === "string" ? body.error : undefined
  return new ArenaApiError(response.status, serverMessage)
}

export class ArenaService {
  constructor(private api: IApiAdapter) {}

  /** `GET /v1/problems/test-cases/{problemId}/samples` — casos de muestra del problema. */
  async getSamples(problemId: string): Promise<SampleDTO[]> {
    return this.unwrap(await this.api.get<SampleDTO[]>(`/v1/problems/test-cases/${problemId}/samples`))
  }

  /**
   * `POST /v1/submissions` — envía una solución. Devuelve el envío creado (PENDING).
   * @throws {ArenaApiError} con `serverMessage` si el backend rechaza el envío (p.ej. 409 duplicado).
   */
  async createSubmission(request: SubmitSolutionRequest): Promise<SubmissionDTO> {
    return this.unwrap(await this.api.post<SubmissionDTO>("/v1/submissions", request))
  }

  /** `GET /v1/submissions/team/{teamId}` — historial de envíos del equipo. */
  async getTeamSubmissions(teamId: string): Promise<SubmissionDTO[]> {
    return this.unwrap(await this.api.get<SubmissionDTO[]>(`/v1/submissions/team/${teamId}`))
  }

  /** `GET /v1/submissions/{id}` — un envío concreto (usado por el polling de respaldo). */
  async getSubmission(submissionId: string): Promise<SubmissionDTO> {
    return this.unwrap(await this.api.get<SubmissionDTO>(`/v1/submissions/${submissionId}`))
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    if (!response.ok) throw toError(response)
    return response.data
  }
}

/** Servicio listo para usar desde los hooks de la feature. */
export function createArenaService(): ArenaService {
  return new ArenaService(createHttpApi())
}
