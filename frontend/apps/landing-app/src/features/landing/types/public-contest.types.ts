/**
 * Tipos para maratones visibles públicamente en la landing.
 * No contienen información sensible (sin problemas, sin submissions).
 */

export type ContestStatus = "active" | "upcoming" | "past"
export type ContestDifficulty = "Principiante" | "Intermedio" | "Avanzado"

export interface PublicContest {
  id: string
  title: string
  description: string
  status: ContestStatus
  startDate: string   // ISO 8601 o string formateado para display
  endDate: string
  duration: string    // e.g. "3 horas"
  participantCount: number
  problemCount: number
  difficulty: ContestDifficulty
}
