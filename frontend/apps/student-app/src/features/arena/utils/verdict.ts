/**
 * Veredictos, estados de juicio y reglas de estado ICPC por problema.
 */
import type {
  SubmissionDTO,
  SubmissionStatus,
  SubmissionVerdict,
  TeamProblemStatus,
} from "../types/arena.types"

/** Etiquetas en español con la sigla ICPC entre paréntesis. */
export const VERDICT_LABELS: Record<SubmissionVerdict, string> = {
  PENDING: "Pendiente",
  ACCEPTED: "Aceptado (AC)",
  WRONG_ANSWER: "Respuesta incorrecta (WA)",
  TIME_LIMIT_EXCEEDED: "Tiempo excedido (TLE)",
  MEMORY_LIMIT_EXCEEDED: "Memoria excedida (MLE)",
  RUNTIME_ERROR: "Error de ejecución (RTE)",
  COMPILATION_ERROR: "Error de compilación (CE)",
  JUDGE_ERROR: "Error del juez",
}

/** Etiqueta corta (sin siglas), útil en tablas estrechas. */
export const VERDICT_SHORT_LABELS: Record<SubmissionVerdict, string> = {
  PENDING: "Pendiente",
  ACCEPTED: "Aceptado",
  WRONG_ANSWER: "Respuesta incorrecta",
  TIME_LIMIT_EXCEEDED: "Tiempo excedido",
  MEMORY_LIMIT_EXCEEDED: "Memoria excedida",
  RUNTIME_ERROR: "Error de ejecución",
  COMPILATION_ERROR: "Error de compilación",
  JUDGE_ERROR: "Error del juez",
}

/** Colores con tokens del tema: verde aceptado, ámbar pendiente, rojo el resto. */
export function verdictTone(verdict: SubmissionVerdict): string {
  switch (verdict) {
    case "ACCEPTED":
      return "text-primary border-primary/30 bg-primary/10"
    case "PENDING":
      return "text-warning border-warning/30 bg-warning/10"
    default:
      return "text-destructive border-destructive/40 bg-destructive/10"
  }
}

/** Estados en los que el juez ya no va a cambiar el envío. */
export function isTerminalStatus(status: SubmissionStatus): boolean {
  return status === "JUDGED" || status === "ENQUEUE_ERROR"
}

/** Estados de envío todavía en curso. */
export function isInFlightStatus(status: SubmissionStatus): boolean {
  return status === "PENDING" || status === "QUEUED" || status === "JUDGING"
}

/** Texto para el estado de un envío: prioriza el juicio en curso y el error de encolado. */
export function submissionStateLabel(submission: Pick<SubmissionDTO, "status" | "verdict">): string {
  if (submission.status === "JUDGING") return "En juicio"
  if (submission.status === "QUEUED") return "En cola"
  if (submission.status === "PENDING") return "Pendiente"
  if (submission.status === "ENQUEUE_ERROR") return VERDICT_LABELS.JUDGE_ERROR
  return VERDICT_LABELS[submission.verdict]
}

/** Colores para `submissionStateLabel`. */
export function submissionStateTone(submission: Pick<SubmissionDTO, "status" | "verdict">): string {
  if (isInFlightStatus(submission.status)) return verdictTone("PENDING")
  if (submission.status === "ENQUEUE_ERROR") return verdictTone("JUDGE_ERROR")
  return verdictTone(submission.verdict)
}

/**
 * Estado del equipo en un problema (reglas ICPC):
 *   1. "Aceptado" si algún envío del problema tiene veredicto ACCEPTED.
 *   2. "Pendiente" si hay algún envío en PENDING/QUEUED/JUDGING.
 *   3. Si no, el veredicto del último envío.
 *   4. Sin envíos -> "Sin enviar".
 */
export function teamProblemStatus(
  submissions: readonly SubmissionDTO[],
  problemId: string
): TeamProblemStatus {
  const own = submissions.filter((s) => s.problemId === problemId)
  if (own.length === 0) return { kind: "none" }
  if (own.some((s) => s.verdict === "ACCEPTED")) return { kind: "accepted" }
  if (own.some((s) => isInFlightStatus(s.status))) return { kind: "pending" }
  const last = own.reduce((a, b) => (Date.parse(b.submittedAt) > Date.parse(a.submittedAt) ? b : a))
  return { kind: "verdict", verdict: last.verdict }
}

/** Texto y color del estado del equipo para un problema. */
export function teamProblemStatusView(status: TeamProblemStatus): { label: string; tone: string } {
  switch (status.kind) {
    case "accepted":
      return { label: VERDICT_SHORT_LABELS.ACCEPTED, tone: verdictTone("ACCEPTED") }
    case "pending":
      return { label: "Pendiente", tone: verdictTone("PENDING") }
    case "verdict":
      return { label: VERDICT_SHORT_LABELS[status.verdict], tone: verdictTone(status.verdict) }
    case "none":
      return { label: "Sin enviar", tone: "text-muted-foreground border-border bg-muted" }
  }
}
