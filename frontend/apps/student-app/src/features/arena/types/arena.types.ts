/**
 * Tipos de la feature `arena` (vista de competencia de una maratón).
 *
 * Los DTO mapean el submission-service (`/api/v1/submissions/**`) y los
 * endpoints de muestras del problem-service. Los tipos de maratón, equipo y
 * problema se reutilizan de `features/contests` (solo vía su index.ts).
 */

/** Lenguajes que acepta `POST /v1/submissions`. */
export type SubmissionLanguage = "C" | "CPP" | "JAVA" | "PYTHON"

export type SubmissionVerdict =
  | "PENDING"
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "RUNTIME_ERROR"
  | "COMPILATION_ERROR"
  | "JUDGE_ERROR"

export type SubmissionStatus = "PENDING" | "QUEUED" | "JUDGING" | "JUDGED" | "ENQUEUE_ERROR"

/** `SubmissionDTO` de `GET /v1/submissions/team/{teamId}`, `GET /v1/submissions/{id}` y `POST /v1/submissions` (201). */
export interface SubmissionDTO {
  id: string
  teamId: string
  problemId: string
  language: SubmissionLanguage
  sourceCode: string
  verdict: SubmissionVerdict
  status: SubmissionStatus
  executionTimeMs: number | null
  memoryUsedKb: number | null
  codeSizeBytes: number
  /** ISO-8601 (Instant). */
  submittedAt: string
}

/** Body de `POST /v1/submissions`. */
export interface SubmitSolutionRequest {
  teamId: string
  problemId: string
  language: SubmissionLanguage
  sourceCode: string
}

/** Caso de prueba de muestra (`GET /v1/problems/test-cases/{problemId}/samples`). */
export interface SampleDTO {
  id: string
  problemId: string
  input: string
  output: string
  expectedOutput: string
  orderIndex: number
  isSample: boolean
}

/** Mensaje que empuja el submission-service por WebSocket al cambiar el estado de un envío. */
export interface SubmissionStatusUpdatedEvent {
  type: "SUBMISSION_STATUS_UPDATED"
  submissionId: string
  problemId: string
  teamId: string
  status: SubmissionStatus
  verdict: SubmissionVerdict
  executionTimeMs: number | null
  memoryUsedKb: number | null
  occurredAt: string
}

/** Problema de la maratón enriquecido para la UI de la arena. */
export interface ArenaProblem {
  /** Id de la relación contest-problem: el que usan los envíos. */
  id: string
  /** Id del problema en el catálogo: el que usan el enunciado y las muestras. */
  problemId: string
  /** Letra mostrada (A, B, C…) según la posición al ordenar por `orderIndex`. */
  letter: string
  /** Alias guardado por el backend (puede coincidir con la letra). */
  alias: string
  orderIndex: number
  points: number
  /** Título del problema; `null` si el problem-service no respondió. */
  title: string | null
  /** Valor crudo de `difficult` (800..3500, múltiplos de 100); `null` si no se cargó. */
  difficulty: number | null
}

/** Estado ICPC de un problema para el equipo. */
export type TeamProblemStatus =
  | { kind: "accepted" }
  | { kind: "pending" }
  | { kind: "verdict"; verdict: SubmissionVerdict }
  | { kind: "none" }
