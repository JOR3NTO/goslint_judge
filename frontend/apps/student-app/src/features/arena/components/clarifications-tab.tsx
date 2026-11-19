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
import { Clock, Globe, Lock, Megaphone, MessageSquare } from "lucide-react"
import { useClarifications } from "../hooks/use-clarifications"
import type { ArenaProblem, ClarificationDTO } from "../types/arena.types"
import { formatDateTimeFull, formatRelativeTime } from "../utils/format"
import { AskClarificationForm } from "./ask-clarification-form"

const ALL_PROBLEMS = "all"
/** Valor del filtro para lo que no pertenece a ningún problema (`problemId === null`). */
const GENERAL = "general"

interface ClarificationsTabProps {
  contestId: string
  problems: ArenaProblem[]
  /** Problema abierto en la pestaña Problemas (se preselecciona en el formulario). */
  initialProblemId: string | null
}

/**
 * ¿El elemento pasa el filtro? Con un problema elegido se muestran sus aclaraciones y,
 * además, los anuncios generales (aplican a todos los problemas).
 */
function matchesFilter(item: ClarificationDTO, filter: string): boolean {
  if (filter === ALL_PROBLEMS) return true
  if (filter === GENERAL) return item.problemId === null
  return item.problemId === filter || (item.kind === "ANNOUNCEMENT" && item.problemId === null)
}

/**
 * Pestaña Aclaraciones (regla ICPC): anuncios del juez destacados arriba y, debajo, las
 * preguntas visibles para el equipo: respuestas públicas de cualquiera y las propias (`mine`).
 * El servidor ya filtra; el servicio vuelve a filtrar (`visibility === "PUBLIC" || mine`).
 * `ClarificationDTO.problemId` es `ArenaProblem.id` (contest-problem) o `null` (general).
 */
export function ClarificationsTab({ contestId, problems, initialProblemId }: ClarificationsTabProps) {
  const { items, isLoading, error, reload, ask } = useClarifications(contestId)
  const [filter, setFilter] = useState<string>(ALL_PROBLEMS)

  const problemsById = useMemo(() => new Map(problems.map((p) => [p.id, p])), [problems])
  const visible = items.filter((item) => matchesFilter(item, filter))
  const announcements = visible.filter((item) => item.kind === "ANNOUNCEMENT")
  const questions = visible.filter((item) => item.kind !== "ANNOUNCEMENT")
  // Solo cuentan las preguntas propias sin responder.
  const myPending = items.filter((item) => item.mine && item.kind === "QUESTION" && item.status === "PENDING").length
  const problemOf = (item: ClarificationDTO) => (item.problemId ? problemsById.get(item.problemId) ?? null : null)

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
                : myPending > 0
                  ? `${myPending} ${myPending === 1 ? "pregunta tuya" : "preguntas tuyas"} sin responder.`
                  : "Tu equipo no tiene preguntas sin responder."}
            </CardDescription>
          </div>

          <div className="w-full sm:w-64">
            <Select value={filter} onValueChange={setFilter}>
              <SelectTrigger aria-label="Filtrar por problema" className="w-full border-border bg-background">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_PROBLEMS}>Todos los problemas</SelectItem>
                <SelectItem value={GENERAL}>General</SelectItem>
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
              {items.length === 0 ? "Aún no hay aclaraciones en esta maratón." : "No hay aclaraciones para este filtro."}
            </p>
          ) : (
            <div className="space-y-4">
              {announcements.length > 0 && (
                <ul aria-label="Anuncios del juez" className="space-y-3">
                  {announcements.map((item) => (
                    <AnnouncementItem key={item.id} item={item} problem={problemOf(item)} />
                  ))}
                </ul>
              )}
              {questions.length > 0 && (
                <ul aria-label="Preguntas" className="space-y-3">
                  {questions.map((item) => (
                    <ClarificationItem key={item.id} item={item} problem={problemOf(item)} />
                  ))}
                </ul>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

/** Letra y título del problema, o "General" si no pertenece a ninguno. */
function ProblemLabel({ problemId, problem }: { problemId: string | null; problem: ArenaProblem | null }) {
  if (problemId === null) {
    return <span className="text-sm font-semibold text-foreground">General</span>
  }
  return (
    <>
      <span className="font-mono font-bold text-primary">{problem?.letter ?? "?"}</span>
      {problem && (
        <span className="min-w-0 flex-1 truncate text-sm text-muted-foreground">
          {problem.title ?? `Problema ${problem.alias}`}
        </span>
      )}
    </>
  )
}

/** Anuncio del juez: destacado, con icono y siempre por encima de las preguntas. */
function AnnouncementItem({ item, problem }: { item: ClarificationDTO; problem: ArenaProblem | null }) {
  return (
    <li className="min-w-0 space-y-2 rounded-lg border border-warning/40 bg-warning/10 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Megaphone className="h-4 w-4 shrink-0 text-warning" aria-hidden="true" />
        <span className="text-xs font-semibold uppercase tracking-wide text-warning">Anuncio del juez</span>
        <span aria-hidden="true" className="text-muted-foreground">·</span>
        <ProblemLabel problemId={item.problemId} problem={problem} />
      </div>
      {item.answer && <p className="whitespace-pre-wrap break-words text-foreground">{item.answer}</p>}
      <time dateTime={item.createdAt} className="block text-xs text-muted-foreground">
        Publicado {formatDateTimeFull(item.createdAt)} · {formatRelativeTime(item.createdAt)}
      </time>
    </li>
  )
}

/** Etiqueta de estado/visibilidad de una pregunta. */
function QuestionBadge({ item }: { item: ClarificationDTO }) {
  if (item.mine && item.status === "PENDING") {
    return (
      <Badge variant="outline" className="gap-1 whitespace-nowrap border-warning/30 bg-warning/10 text-warning">
        <Clock className="h-3 w-3" aria-hidden="true" />
        Esperando respuesta
      </Badge>
    )
  }
  if (item.visibility === "PUBLIC") {
    return (
      <Badge variant="outline" className="gap-1 whitespace-nowrap border-primary/30 bg-primary/10 text-primary">
        <Globe className="h-3 w-3" aria-hidden="true" />
        Respuesta pública
      </Badge>
    )
  }
  return (
    <Badge variant="outline" className="gap-1 whitespace-nowrap border-border bg-muted text-foreground">
      <Lock className="h-3 w-3" aria-hidden="true" />
      Solo tu equipo
    </Badge>
  )
}

function ClarificationItem({ item, problem }: { item: ClarificationDTO; problem: ArenaProblem | null }) {
  const answered = item.status === "ANSWERED"

  return (
    <li
      className={cn(
        "min-w-0 space-y-3 rounded-lg border bg-card p-4",
        item.mine ? "border-primary/40" : "border-border"
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <ProblemLabel problemId={item.problemId} problem={problem} />
        {item.mine && (
          <Badge variant="outline" className="border-primary/30 bg-primary/10 text-primary">
            Tu equipo
          </Badge>
        )}
        <QuestionBadge item={item} />
      </div>

      {item.question && (
        <div className="space-y-1">
          <p className="whitespace-pre-wrap break-words text-foreground">{item.question}</p>
          <time dateTime={item.createdAt} className="text-xs text-muted-foreground">
            Enviada {formatDateTimeFull(item.createdAt)} · {formatRelativeTime(item.createdAt)}
          </time>
        </div>
      )}

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
