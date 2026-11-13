/**
 * ClarificationsService — adapter de la feature `arena` para las aclaraciones del contest-service
 * (`/v1/contests/{id}/clarifications` y `/v1/contest-problems/{id}/clarifications`).
 *
 * Depende solo de `IApiAdapter`. Las rutas son relativas: `HttpApiAdapter` antepone `/api`.
 */
import { createHttpApi } from "@/core/api"
import type { ApiResponse, IApiAdapter } from "@/core/api"
import type { ClarificationDTO } from "../types/arena.types"
import { ArenaApiError } from "./arena-service"

/** Límites de la pregunta en la UI. El backend acepta hasta 2000 caracteres. */
export const CLARIFICATION_MIN_LENGTH = 3
export const CLARIFICATION_MAX_LENGTH = 500

/**
 * Extrae el mensaje del cuerpo de error: el backend usa `{ message }` (ErrorResponseDTO),
 * el mock también `{ error }`, y las validaciones por campo llegan como `{ question: "..." }`.
 */
function toError(response: ApiResponse<unknown>): ArenaApiError {
  const body = response.data
  let serverMessage: string | undefined
  if (body && typeof body === "object") {
    const record = body as Record<string, unknown>
    const candidate =
      [record.error, record.message].find((v): v is string => typeof v === "string" && v.length > 0) ??
      Object.values(record).find((v): v is string => typeof v === "string" && v.length > 0)
    serverMessage = candidate
  }
  return new ArenaApiError(response.status, serverMessage)
}

export class ClarificationsService {
  constructor(private api: IApiAdapter) {}

  /** `GET /v1/contests/{contestId}/clarifications` — todas las aclaraciones de la maratón (pública), más reciente primero. */
  async getByContest(contestId: string): Promise<ClarificationDTO[]> {
    return this.unwrap(await this.api.get<ClarificationDTO[]>(`/v1/contests/${contestId}/clarifications`))
  }

  /**
   * `POST /v1/contest-problems/{contestProblemId}/clarifications` — pregunta en nombre del equipo.
   * `contestProblemId` es `ArenaProblem.id` (la relación contest-problem), así lo exige el backend.
   * @throws {ArenaApiError} con `serverMessage` si el backend rechaza la pregunta.
   */
  async ask(contestProblemId: string, question: string): Promise<ClarificationDTO> {
    return this.unwrap(
      await this.api.post<ClarificationDTO>(`/v1/contest-problems/${contestProblemId}/clarifications`, { question })
    )
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    if (!response.ok) throw toError(response)
    return response.data
  }
}

/** Servicio listo para usar desde los hooks de la feature. */
export function createClarificationsService(): ClarificationsService {
  return new ClarificationsService(createHttpApi())
}
