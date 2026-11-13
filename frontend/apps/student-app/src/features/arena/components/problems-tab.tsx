"use client"

import type { ArenaProblem, TeamProblemStatus } from "../types/arena.types"
import type { UseProblemDetailReturn } from "../hooks/use-problem-detail"
import { ProblemList } from "./problem-list"
import { ProblemStatement } from "./problem-statement"
import { SubmitPanel, type SubmitPanelProps } from "./submit-panel"

interface ProblemsTabProps {
  problems: ArenaProblem[]
  selected: ArenaProblem | null
  statusOf: (problemId: string) => TeamProblemStatus
  onSelect: (problemId: string) => void
  detail: UseProblemDetailReturn
  submit: Omit<SubmitPanelProps, "problemId" | "letter">
}

/**
 * Pestaña Problemas: tres columnas en pantallas grandes (lista, enunciado, envío);
 * en pantallas chicas las columnas se apilan en ese orden.
 */
export function ProblemsTab({
  problems,
  selected,
  statusOf,
  onSelect,
  detail,
  submit,
}: ProblemsTabProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)_minmax(0,1fr)] lg:items-start">
      <ProblemList
        problems={problems}
        selectedProblemId={selected?.problemId ?? null}
        statusOf={statusOf}
        onSelect={onSelect}
      />

      <ProblemStatement
        letter={selected?.letter ?? null}
        problem={detail.problem}
        samples={detail.samples}
        isLoading={detail.isLoading}
        error={detail.error}
        samplesFailed={detail.samplesFailed}
        onRetry={detail.reload}
      />

      {selected ? (
        <SubmitPanel problemId={selected.problemId} letter={selected.letter} {...submit} />
      ) : (
        <div className="rounded-lg border border-border bg-card p-6 text-muted-foreground">
          No hay problemas disponibles para enviar.
        </div>
      )}
    </div>
  )
}
