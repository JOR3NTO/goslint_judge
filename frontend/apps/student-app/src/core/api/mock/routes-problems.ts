/**
 * Rutas del problem-service en el mock: enunciado y casos de muestra.
 */
import { ApiRouter, httpError, ok } from "./router"
import type { MockContext } from "./router"

export function registerProblemRoutes(router: ApiRouter): void {
  router.add("GET", "/v1/problems/:id", (req, ctx: MockContext) => {
    const problem = ctx.state.problems.find((p) => p.id === req.params.id)
    if (!problem) throw httpError(404, `El problema con ID '${req.params.id}' no existe.`)
    return ok({ ...problem })
  })

  router.add("GET", "/v1/problems/test-cases/:problemId/samples", (req, ctx: MockContext) => {
    const samples = ctx.state.samples
      .filter((s) => s.problemId === req.params.problemId && s.isSample)
      .sort((a, b) => a.orderIndex - b.orderIndex)
      .map((s) => ({ ...s }))
    return ok(samples)
  })
}
