"use client"

import { useMemo, useState } from "react"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/primitives/select"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { cn } from "@/shared/utils/cn"
import { MessageSquare } from "lucide-react"
import { useClarifications } from "../hooks/use-clarifications"
import type { ArenaProblem, ClarificationDTO } from "../types/arena.types"
import { formatDateTimeFull, formatRelativeTime } from "../utils/format"
import { AskClarificationForm } from "./ask-clarification-form"

const ALL_PROBLEMS = "all"

interface ClarificationsTabProps {
  contestId: string
  problems: ArenaProblem[]
  /** Id del equipo del usuario en esta maratón, para marcar sus preguntas. */
  myTeamId: string | null
  /** Problema abierto en la pestaña Problemas (se preselecciona en el formulario). */
  initialProblemId: string | null
}

/**
 * Pestaña Aclaraciones: formulario para preguntar y lista de aclaraciones de la maratón,
 * filtrable por problema. `ClarificationDTO.problemId` es `ArenaProblem.id` (contest-problem).
 */
export function ClarificationsTab({ contestId, problems, myTeamId, initialProblemId }: ClarificationsTabProps) {
  const { items, isLoading, error, reload, ask } = useClarifications(contestId)
  const [filter, setFilter] = useState<string>(ALL_PROBLEMS)

  const problemsById = useMemo(() => new Map(problems.map((p) => [p.id, p])), [problems])
  const visible = filter === ALL_PROBLEMS ? items : items.filter((item) => item.problemId === filter)
  const pending = items.filter((item) => item.status === "PENDING").length

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,22rem)_minmax(0,1fr)] lg:items-start">
      <AskClarificationForm problems={problems} initialProblemId={initialProblemId} onAsk={ask} />

      <Card className="min-w-0 border-border bg-card">
        <CardHeader className="flex-row flex-wrap items-start justify-between gap-3 space-y-0">
          <div className="min-w-0 space-y-1.5">
            <CardTitle className="flex items-center gap-2">
              <MessageSquare className="h-5 w-5 text-primary" />
              Aclaraciones ({items.length})
            </CardTitle>
            <CardDescription>
              {items.length === 0
                ? "Pregunta sobre cualquier problema de la maratón."
                : pending > 0
                  ? `${pending} sin responder.`
                  : "Todas las preguntas tienen respuesta."}
            </CardDescription>
          </div>

          <div className="w-full sm:w-64">
            <Select value={filter} onValueChange={setFilter}>
              <SelectTrigger aria-label="Filtrar por problema" className="w-full border-border bg-background">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_PROBLEMS}>Todos los problemas</SelectItem>
                {problems.map((problem) => (
                  <SelectItem key={problem.id} value={problem.id}>
                    {problem.letter} · {problem.title ?? `Problema ${problem.alias}`}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-24 w-full" />
            </div>
          ) : error && items.length === 0 ? (
            <div className="space-y-3">
              <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
                No se pudieron cargar las aclaraciones: {error}
              </p>
              <Button type="button" variant="outline" onClick={reload}>
                Reintentar
              </Button>
            </div>
          ) : visible.length === 0 ? (
            <p className="py-8 text-center text-muted-foreground">
              {items.length === 0
                ? "Aún no hay aclaraciones en esta maratón."
                : "Aún no hay aclaraciones para este problema."}
            </p>
          ) : (
            <ul className="space-y-3">
              {visible.map((item) => (
                <ClarificationItem
                  key={item.id}
                  item={item}
                  problem={problemsById.get(item.problemId) ?? null}
                  mine={item.teamId === myTeamId}
                />
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function ClarificationItem({
  item,
  problem,
  mine,
}: {
  item: ClarificationDTO
  problem: ArenaProblem | null
  mine: boolean
}) {
  const answered = item.status === "ANSWERED"

  return (
    <li
      className={cn(
        "min-w-0 space-y-3 rounded-lg border bg-card p-4",
        mine ? "border-primary/40" : "border-border"
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-mono font-bold text-primary">{problem?.letter ?? "?"}</span>
        {problem && (
          <span className="min-w-0 flex-1 truncate text-sm text-muted-foreground">
            {problem.title ?? `Problema ${problem.alias}`}
          </span>
        )}
        {mine && (
          <Badge variant="outline" className="border-primary/30 bg-primary/10 text-primary">
            Tu equipo
          </Badge>
        )}
        <Badge
          variant="outline"
          className={cn(
            "whitespace-nowrap",
            answered
              ? "border-primary/30 bg-primary/10 text-primary"
              : "border-warning/30 bg-warning/10 text-warning"
          )}
        >
          {answered ? "Respondida" : "Sin responder"}
        </Badge>
      </div>

      <div className="space-y-1">
        <p className="whitespace-pre-wrap break-words text-foreground">{item.question}</p>
        <time dateTime={item.createdAt} className="text-xs text-muted-foreground">
          Enviada {formatDateTimeFull(item.createdAt)} · {formatRelativeTime(item.createdAt)}
        </time>
      </div>

      {answered && item.answer && (
        <div className="space-y-1 rounded-md border border-primary/30 bg-primary/10 p-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">Respuesta del organizador</p>
          <p className="whitespace-pre-wrap break-words text-foreground">{item.answer}</p>
          {item.answeredAt && (
            <time dateTime={item.answeredAt} className="text-xs text-muted-foreground">
              Respondida {formatDateTimeFull(item.answeredAt)}
            </time>
          )}
        </div>
      )}
    </li>
  )
}
