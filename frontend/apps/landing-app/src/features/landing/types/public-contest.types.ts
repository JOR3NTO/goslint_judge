/**
 * Tipos de las maratones públicas que muestra la landing.
 *
 * Contrato del backend: GET {API_URL}/api/v1/contests (público, sin autenticación).
 * Respuesta: un arreglo de ApiContest. Solo se usan los campos listados aquí.
 */

/** Estado tal como lo devuelve el backend (enum ContestStatus). */
export type ApiContestStatus = "UPCOMING" | "ACTIVE" | "ENDED"

/** Elemento crudo del backend (ContestResponseDTO). Fechas en ISO 8601 (Instant). */
export interface ApiContest {
  id: string
  title: string
  description: string | null
  startTime: string
  endTime: string
  status: ApiContestStatus
}

/** Pestañas de la landing. */
export type ContestStatus = "active" | "upcoming" | "past"

/** Maratón ya normalizada para la UI. */
export interface PublicContest {
  id: string
  title: string
  description: string
  status: ContestStatus
  startTime: Date
  endTime: Date
}
