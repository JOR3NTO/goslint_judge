/**
 * Tipos de eventos del dominio — el contrato de eventos que circulan por el bus.
 *
 * Convención de nombres: DOMINIO_VERBO_ESTADO (en snake_case mayúscula).
 *
 * Cada feature que quiera publicar o suscribirse a eventos del bus
 * solo depende de estos tipos, nunca de la implementación del bus.
 *
 * Para agregar un nuevo evento:
 *  1. Define su nombre como clave de DomainEvents (p.ej. "CLARIFICATION_POSTED")
 *  2. Define el tipo de su payload
 *  3. El bus lo propagará automáticamente a todos los suscriptores del evento
 */

// ─── Payloads de eventos ────────────────────────────────────────────────────

export interface SubmissionVerdictPayload {
  submissionId: string
  contestId: string
  problemId: string
  userId: string
  verdict: "ACCEPTED" | "WRONG_ANSWER" | "TIME_LIMIT_EXCEEDED" | "RUNTIME_ERROR" | "COMPILATION_ERROR" | "PENDING"
  executionTimeMs?: number
  memoryUsedKb?: number
  timestamp: string
}

export interface RankingUpdatePayload {
  contestId: string
  /** Lista parcial o completa de participantes con su nuevo ranking */
  entries: Array<{
    userId: string
    username: string
    rank: number
    solved: number
    penalty: number
  }>
  timestamp: string
}

export interface ClarificationPostedPayload {
  clarificationId: string
  contestId: string
  problemId: string
  question: string
  answer?: string
  isPublic: boolean
  timestamp: string
}

export interface ContestStatusChangedPayload {
  contestId: string
  previousStatus: "upcoming" | "active" | "ended"
  newStatus: "upcoming" | "active" | "ended"
  timestamp: string
}

// ─── Mapa de eventos → payloads ─────────────────────────────────────────────

/**
 * DomainEvents — registra cada nombre de evento con el tipo exacto de su payload.
 * Garantiza type-safety en publish() y subscribe().
 */
export interface DomainEvents {
  SUBMISSION_VERDICT:       SubmissionVerdictPayload
  RANKING_UPDATED:          RankingUpdatePayload
  CLARIFICATION_POSTED:     ClarificationPostedPayload
  CONTEST_STATUS_CHANGED:   ContestStatusChangedPayload
}

export type DomainEventName = keyof DomainEvents
