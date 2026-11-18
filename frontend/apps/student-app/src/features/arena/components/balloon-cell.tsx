import { Balloon } from "./balloon"
import type { ArenaProblem } from "../types/arena.types"

/** Máximo de globos visibles por equipo antes de resumir con "+N". */
export const MAX_VISIBLE_BALLOONS = 12

interface BalloonCellProps {
  solvedProblemIds: readonly string[]
  /** Problemas de la maratón, ya ordenados por `orderIndex`. */
  problems: ArenaProblem[]
  /** Ids de problema recién resueltos (animan al aparecer). */
  freshIds: readonly string[]
  celebrate: boolean
  teamName: string
  /** El backend no informa los problemas de este equipo: no se afirma nada. */
  unknown?: boolean
}

/**
 * Globos de un equipo, uno por problema resuelto. Orden: el de la maratón (`orderIndex`, A, B, C...), el mismo
 * para todos los equipos, de modo que las columnas se comparan a simple vista.
 */
export function BalloonCell({ solvedProblemIds, problems, freshIds, celebrate, teamName, unknown = false }: BalloonCellProps) {
  const solved = new Set(solvedProblemIds)
  const items = problems.filter((p) => solved.has(p.problemId))
  // Los nuevos nunca se ocultan tras el "+N".
  const visible = items.filter((p, i) => i < MAX_VISIBLE_BALLOONS || freshIds.includes(p.problemId))
  const hidden = items.length - visible.length

  if (items.length === 0) {
    return (
      <span className="text-xs text-muted-foreground" aria-label={unknown ? undefined : `${teamName}: sin problemas resueltos`}>
        -
      </span>
    )
  }

  return (
    <div role="list" aria-label={`Problemas resueltos por ${teamName}`} className="flex flex-wrap items-end gap-x-0.5 gap-y-1 py-1">
      {visible.map((problem) => (
        <Balloon
          key={problem.problemId}
          color={problem.balloonColor}
          label={`Problema ${problem.alias} resuelto${problem.title ? `: ${problem.title}` : ""}`}
          isNew={freshIds.includes(problem.problemId)}
          celebrate={celebrate}
        />
      ))}
      {hidden > 0 && (
        <span role="listitem" className="self-center pl-1 font-mono text-xs text-muted-foreground">
          +{hidden}
        </span>
      )}
    </div>
  )
}
