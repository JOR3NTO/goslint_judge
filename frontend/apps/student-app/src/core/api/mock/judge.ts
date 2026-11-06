/**
 * Juez simulado: decide el veredicto a partir del código y resuelve en qué estado
 * está un envío según el tiempo transcurrido desde `submittedAt`.
 *
 * Así el polling del front ve la evolución PENDING → QUEUED → JUDGING → JUDGED sin
 * necesidad de WebSocket ni de temporizadores en el servidor.
 */
import type {
  SubmissionLanguage,
  SubmissionStatus,
  SubmissionVerdict,
} from "@/features/arena/types/arena.types"
import type { ProblemDTO } from "@/features/contests/types/contest.types"
import type { SubmissionRecord } from "./model"
import { pseudoRandom } from "./util"

export const SUBMISSION_LANGUAGES: readonly SubmissionLanguage[] = ["C", "CPP", "JAVA", "PYTHON"]

/** Milisegundos desde `submittedAt` en los que cada estado empieza. */
export const QUEUED_AFTER_MS = 1500
export const JUDGING_AFTER_MS = 3000
export const JUDGED_AFTER_MS = 6000

/**
 * Reglas del veredicto simulado, en este orden:
 *  1. Código casi vacío (menos de 20 caracteres sin espacios) o con "TODO" → WRONG_ANSWER.
 *  2. Bucle infinito ("while(true)" o "while True") → TIME_LIMIT_EXCEEDED.
 *  3. "syntax_error" en el código → COMPILATION_ERROR.
 *  4. Cualquier otro caso → ACCEPTED.
 */
export function simulateVerdict(sourceCode: string): SubmissionVerdict {
  const compact = sourceCode.replace(/\s+/g, "")
  if (compact.length < 20 || sourceCode.includes("TODO")) return "WRONG_ANSWER"
  if (/while\s*\(\s*true\s*\)|while\s+True\s*:/.test(sourceCode)) return "TIME_LIMIT_EXCEEDED"
  if (sourceCode.includes("syntax_error")) return "COMPILATION_ERROR"
  return "ACCEPTED"
}

export interface SubmissionResolution {
  status: SubmissionStatus
  verdict: SubmissionVerdict
  executionTimeMs: number | null
  memoryUsedKb: number | null
}

/** Estado y veredicto de un envío en el instante `now`. */
export function resolveSubmission(
  record: SubmissionRecord,
  now: number,
  problem: ProblemDTO | undefined
): SubmissionResolution {
  const elapsed = now - Date.parse(record.submittedAt)
  const unjudged = { executionTimeMs: null, memoryUsedKb: null }

  if (elapsed < QUEUED_AFTER_MS) return { status: "PENDING", verdict: "PENDING", ...unjudged }
  if (elapsed < JUDGING_AFTER_MS) return { status: "QUEUED", verdict: "PENDING", ...unjudged }
  if (elapsed < JUDGED_AFTER_MS) return { status: "JUDGING", verdict: "PENDING", ...unjudged }

  const verdict = record.verdictOverride ?? simulateVerdict(record.sourceCode)
  return {
    status: "JUDGED",
    verdict,
    ...metricsFor(record.id, verdict, problem?.timeLimitMs ?? 1000, problem?.memoryLimitKb ?? 131072),
  }
}

/** Tiempo y memoria pseudoaleatorios pero estables para cada envío. */
function metricsFor(
  id: string,
  verdict: SubmissionVerdict,
  timeLimitMs: number,
  memoryLimitKb: number
): { executionTimeMs: number | null; memoryUsedKb: number | null } {
  switch (verdict) {
    case "COMPILATION_ERROR":
    case "JUDGE_ERROR":
      return { executionTimeMs: null, memoryUsedKb: null }
    case "TIME_LIMIT_EXCEEDED":
      return { executionTimeMs: timeLimitMs, memoryUsedKb: pseudoRandom(`${id}:m`, 8000, 60000) }
    case "MEMORY_LIMIT_EXCEEDED":
      return { executionTimeMs: pseudoRandom(`${id}:t`, 40, 400), memoryUsedKb: memoryLimitKb }
    case "RUNTIME_ERROR":
      return { executionTimeMs: pseudoRandom(`${id}:t`, 10, 200), memoryUsedKb: pseudoRandom(`${id}:m`, 4000, 30000) }
    default:
      return {
        executionTimeMs: pseudoRandom(`${id}:t`, 20, Math.max(20, Math.floor(timeLimitMs * 0.6))),
        memoryUsedKb: pseudoRandom(`${id}:m`, 4000, Math.max(4000, Math.floor(memoryLimitKb * 0.5))),
      }
  }
}

/** Veredictos que cuentan como intento fallido en el ranking ICPC (no lo son los de compilación). */
export const COUNTABLE_WRONG_VERDICTS: ReadonlySet<SubmissionVerdict> = new Set<SubmissionVerdict>([
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "MEMORY_LIMIT_EXCEEDED",
  "RUNTIME_ERROR",
])
