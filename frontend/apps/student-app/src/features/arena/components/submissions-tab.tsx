"use client"

import type { KeyboardEvent } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/primitives/table"
import { History } from "lucide-react"
import type { ArenaProblem, SubmissionDTO } from "../types/arena.types"
import { LANGUAGE_LABELS } from "../utils/language"
import { formatDateTimeFull, formatKb, formatMs } from "../utils/format"
import { SubmissionStateBadge } from "./verdict-badge"

interface SubmissionsTabProps {
  submissions: SubmissionDTO[]
  /** Problemas de la maratón indexados por `id` (contest-problem), para mostrar letra y título. */
  problemsById: Map<string, ArenaProblem>
  isLoading: boolean
  error: string | null
  onSelect: (submissionId: string) => void
}

/** Pestaña Mis envíos: todos los envíos del equipo, más reciente primero. */
export function SubmissionsTab({ submissions, problemsById, isLoading, error, onSelect }: SubmissionsTabProps) {
  return (
    <Card className="border-border bg-card">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <History className="h-5 w-5 text-primary" />
          Mis envíos ({submissions.length})
        </CardTitle>
        <CardDescription>Envíos de tu equipo en esta maratón. Haz clic en uno para ver el código.</CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading && submissions.length === 0 ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : error && submissions.length === 0 ? (
          <p className="text-muted-foreground">No se pudo cargar el historial: {error}</p>
        ) : submissions.length === 0 ? (
          <p className="text-muted-foreground">Tu equipo aún no ha enviado soluciones.</p>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="border-border hover:bg-transparent">
                  <TableHead className="text-muted-foreground">Hora</TableHead>
                  <TableHead className="text-muted-foreground">Problema</TableHead>
                  <TableHead className="text-muted-foreground">Lenguaje</TableHead>
                  <TableHead className="text-muted-foreground">Veredicto</TableHead>
                  <TableHead className="text-right text-muted-foreground">Tiempo</TableHead>
                  <TableHead className="text-right text-muted-foreground">Memoria</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {submissions.map((submission) => {
                  const problem = problemsById.get(submission.problemId)
                  const open = () => onSelect(submission.id)
                  return (
                    <TableRow
                      key={submission.id}
                      tabIndex={0}
                      role="button"
                      aria-label={`Ver envío del ${formatDateTimeFull(submission.submittedAt)}`}
                      onClick={open}
                      onKeyDown={(event: KeyboardEvent<HTMLTableRowElement>) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault()
                          open()
                        }
                      }}
                      className="cursor-pointer border-border hover:bg-secondary/50 focus-visible:bg-secondary/50 focus-visible:outline-none"
                    >
                      <TableCell className="whitespace-nowrap text-muted-foreground">
                        {formatDateTimeFull(submission.submittedAt)}
                      </TableCell>
                      <TableCell>
                        <span className="font-mono text-primary">{problem?.letter ?? "?"}</span>
                        {problem?.title && <span className="ml-2 text-foreground">{problem.title}</span>}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{LANGUAGE_LABELS[submission.language]}</TableCell>
                      <TableCell>
                        <SubmissionStateBadge submission={submission} />
                      </TableCell>
                      <TableCell className="text-right font-mono text-muted-foreground">
                        {formatMs(submission.executionTimeMs)}
                      </TableCell>
                      <TableCell className="text-right font-mono text-muted-foreground">
                        {formatKb(submission.memoryUsedKb)}
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
