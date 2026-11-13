import { Badge } from "@/shared/ui/primitives/badge"
import { cn } from "@/shared/utils/cn"
import type { ArenaProblem, TeamProblemStatus } from "../types/arena.types"
import { DIFFICULTY_TONE, difficultyLabel } from "../utils/difficulty"
import { teamProblemStatusView } from "../utils/verdict"

interface ProblemListProps {
  problems: ArenaProblem[]
  selectedProblemId: string | null
  statusOf: (problemId: string) => TeamProblemStatus
  onSelect: (problemId: string) => void
}

/**
 * Lista de problemas. En móvil es una tira horizontal con desplazamiento (chips);
 * desde `lg` es una columna con filas. El título ocupa hasta 2 líneas.
 */
export function ProblemList({ problems, selectedProblemId, statusOf, onSelect }: ProblemListProps) {
  return (
    <nav
      aria-label="Lista de problemas"
      className="min-w-0 lg:rounded-lg lg:border lg:border-border lg:bg-card"
    >
      <ul className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 lg:mx-0 lg:block lg:divide-y lg:divide-border lg:overflow-visible lg:p-0">
        {problems.map((problem) => {
          const isSelected = problem.problemId === selectedProblemId
          const difficulty = difficultyLabel(problem.difficulty)
          const state = teamProblemStatusView(statusOf(problem.problemId))
          const title = problem.title ?? `Problema ${problem.alias}`
          return (
            <li key={problem.id} className="shrink-0 lg:shrink">
              <button
                type="button"
                onClick={() => onSelect(problem.problemId)}
                aria-current={isSelected ? "true" : undefined}
                title={title}
                className={cn(
                  "flex w-full max-w-[17rem] items-start gap-2.5 rounded-full border border-border bg-card px-2.5 py-2 text-left transition-colors hover:bg-secondary/60",
                  "lg:max-w-none lg:items-center lg:gap-3 lg:rounded-none lg:border-0 lg:border-l-2 lg:border-l-transparent lg:px-3 lg:py-3",
                  isSelected &&
                    "border-primary bg-primary/10 hover:bg-primary/15 lg:border-l-primary lg:border-y-0 lg:border-r-0"
                )}
              >
                <span
                  className={cn(
                    "flex h-7 w-7 shrink-0 items-center justify-center rounded-md border font-mono text-sm font-bold lg:h-9 lg:w-9 lg:text-base",
                    isSelected
                      ? "border-primary/40 bg-primary text-primary-foreground"
                      : "border-primary/30 bg-primary/10 text-primary"
                  )}
                >
                  {problem.letter}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="line-clamp-2 break-words text-sm font-medium leading-snug text-foreground lg:text-base">
                    {title}
                  </span>
                  <span className="mt-1 flex flex-wrap items-center gap-1.5">
                    {difficulty && (
                      <Badge
                        variant="outline"
                        className={cn("hidden text-[11px] lg:inline-flex", DIFFICULTY_TONE[difficulty])}
                      >
                        {difficulty}
                      </Badge>
                    )}
                    <Badge variant="outline" className={cn("text-[11px]", state.tone)}>
                      {state.label}
                    </Badge>
                  </span>
                </span>
              </button>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
