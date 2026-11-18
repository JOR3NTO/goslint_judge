import type { ArenaProblem, SubmissionDTO } from "../types/arena.types"

/**
 * Ids de problema del catálogo (`ArenaProblem.problemId`) con al menos un envío ACEPTADO propio,
 * en orden cronológico del primer aceptado. Los envíos apuntan al problema del concurso (`ArenaProblem.id`).
 */
export function ownSolvedProblemIds(
  submissions: readonly SubmissionDTO[],
  problems: readonly ArenaProblem[]
): string[] {
  const catalogIdOf = new Map(problems.map((p) => [p.id, p.problemId]))
  const firstAccepted = new Map<string, number>()
  for (const s of submissions) {
    if (s.verdict !== "ACCEPTED") continue
    const catalogId = catalogIdOf.get(s.problemId)
    if (!catalogId) continue
    const at = Date.parse(s.submittedAt)
    const prev = firstAccepted.get(catalogId)
    if (prev === undefined || at < prev) firstAccepted.set(catalogId, at)
  }
  return [...firstAccepted.entries()].sort((a, b) => a[1] - b[1]).map(([id]) => id)
}
