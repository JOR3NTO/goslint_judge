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

/** Columna izquierda: problemas con letra, título, dificultad y estado del equipo. */
export function ProblemList({ problems, selectedProblemId, statusOf, onSelect }: ProblemListProps) {
  return (
    <nav aria-label="Lista de problemas" className="rounded-lg border border-border bg-card">
      <ul className="divide-y divide-border">
        {problems.map((problem) => {
          const isSelected = problem.problemId === selectedProblemId
          const difficulty = difficultyLabel(problem.difficulty)
          const state = teamProblemStatusView(statusOf(problem.id))
          return (
            <li key={problem.id}>
              <button
                type="button"
                onClick={() => onSelect(problem.problemId)}
                aria-current={isSelected ? "true" : undefined}
                className={cn(
                  "flex w-full items-center gap-3 px-3 py-3 text-left transition-colors hover:bg-secondary/60",
                  isSelected && "bg-primary/10 hover:bg-primary/15"
                )}
              >
                <span
                  className={cn(
                    "flex h-9 w-9 shrink-0 items-center justify-center rounded-md border font-mono text-base font-bold",
                    isSelected
                      ? "border-primary/40 bg-primary text-primary-foreground"
                      : "border-primary/30 bg-primary/10 text-primary"
                  )}
                >
                  {problem.letter}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-foreground">
                    {problem.title ?? `Problema ${problem.alias}`}
                  </span>
                  <span className="mt-1 flex flex-wrap items-center gap-1.5">
                    {difficulty && (
                      <Badge variant="outline" className={cn("text-[11px]", DIFFICULTY_TONE[difficulty])}>
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
