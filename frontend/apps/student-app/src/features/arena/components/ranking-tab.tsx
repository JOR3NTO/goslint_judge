"use client"

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/primitives/table"
import { cn } from "@/shared/utils/cn"
import { useMemo, useState, type CSSProperties } from "react"
import { RefreshCw, Snowflake, Sparkles, Trophy } from "lucide-react"
import { env } from "@/core/config/env"
import type { TeamDTO } from "@/features/contests"
import { useScoreboard, SCOREBOARD_REFRESH_MS } from "../hooks/use-scoreboard"
import { useNewBalloons } from "../hooks/use-new-balloons"
import type { ArenaProblem } from "../types/arena.types"
import { BalloonCell } from "./balloon-cell"

interface RankingTabProps {
  contestId: string
  /** Solo una maratón activa se refresca sola. */
  contestActive: boolean
  /** Id del equipo del usuario en esta maratón, para resaltarlo. */
  myTeamId: string | null
  /** Problemas de la maratón (con color de globo), ordenados por `orderIndex`. */
  problems: ArenaProblem[]
  /**
   * Problemas resueltos por el equipo del usuario según sus propios envíos aceptados. El backend real no envía
   * `solvedProblemIds`, así que solo el equipo propio muestra globos; los demás solo si el campo llega (mock).
   */
  mySolvedProblemIds: readonly string[]
  /** Los envíos propios ya cargaron (evita animar el historial como si fuera nuevo). */
  submissionsReady: boolean
}

const timeFormatter = new Intl.DateTimeFormat("es-CO", {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
})

/**
 * Posición según el orden del backend, que ya desempata por la hora del último problema resuelto.
 * Solo comparten posición los equipos sin problemas resueltos (1, 2, 3, 4, 4...): entre ellos no hay desempate.
 */
function competitionRanks(teams: TeamDTO[]): number[] {
  let rank = 0
  return teams.map((team, index) => {
    const previous = teams[index - 1]
    if (!previous || team.score > 0 || previous.score !== team.score || previous.penaltyTime !== team.penaltyTime) {
      rank = index + 1
    }
    return rank
  })
}

/** Tono sutil para los tres primeros puestos (solo tokens del tema). */
function positionTone(position: number): string {
  if (position === 1) return "border-warning/40 bg-warning/10 text-warning"
  if (position <= 3) return "border-border bg-secondary text-foreground"
  return "border-transparent text-muted-foreground"
}

/** Pestaña Ranking: tabla de posiciones de la maratón con recarga manual y automática. */
export function RankingTab({ contestId, contestActive, myTeamId, problems, mySolvedProblemIds, submissionsReady }: RankingTabProps) {
  const { teams: fetchedTeams, isLoading, isRefreshing, error, updatedAt, frozen, live, refresh } = useScoreboard(
    contestId,
    contestActive
  )

  // Solo en mock (sin SSE): globos simulados que se superponen a los datos del ranking.
  const [demoSolved, setDemoSolved] = useState<Record<string, string[]>>({})
  const teams = useMemo(
    () =>
      fetchedTeams.map((team) => {
        const extra = [...(demoSolved[team.id] ?? []), ...(team.id === myTeamId ? mySolvedProblemIds : [])]
        if (extra.length === 0) return team
        const base = team.solvedProblemIds ?? []
        return { ...team, solvedProblemIds: [...base, ...new Set(extra.filter((id) => !base.includes(id)))] }
      }),
    [fetchedTeams, demoSolved, myTeamId, mySolvedProblemIds]
  )
  const freshByTeam = useNewBalloons(teams, !isLoading && submissionsReady)
  const problemColor = useMemo(() => new Map(problems.map((p) => [p.problemId, p.balloonColor])), [problems])

  const simulateAccepted = () => {
    const candidates = teams.flatMap((team) =>
      problems
        .filter((p) => !(team.solvedProblemIds ?? []).includes(p.problemId))
        .map((p) => ({ teamId: team.id, problemId: p.problemId }))
    )
    if (candidates.length === 0) return
    const pick = candidates[Math.floor(Math.random() * candidates.length)]
    setDemoSolved((current) => ({ ...current, [pick.teamId]: [...(current[pick.teamId] ?? []), pick.problemId] }))
  }

  const ranks = competitionRanks(teams)
  const allZero = teams.length > 0 && teams.every((team) => team.score === 0)
  const refreshNote = live
    ? "En vivo: se actualiza con cada envío aceptado."
    : contestActive
      ? `Se actualiza sola cada ${SCOREBOARD_REFRESH_MS / 1000} s.`
      : "La maratón no está activa: usa Actualizar para ver cambios."

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex-row flex-wrap items-start justify-between gap-3 space-y-0">
        <div className="min-w-0 space-y-1.5">
          <CardTitle className="flex items-center gap-2">
            <Trophy className="h-5 w-5 text-primary" />
            Ranking
            {frozen && (
              <Badge variant="outline" className="border-border bg-secondary text-foreground">
                <Snowflake className="mr-1 h-3.5 w-3.5" aria-hidden="true" />
                Congelado
              </Badge>
            )}
          </CardTitle>
          <CardDescription>{refreshNote}</CardDescription>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {env.MOCK_ENABLED && problems.length > 0 && (
            <Button type="button" variant="ghost" size="sm" onClick={simulateAccepted} className="text-muted-foreground">
              <Sparkles aria-hidden="true" />
              Simular aceptado
            </Button>
          )}
          {updatedAt && (
            <span className="text-xs text-muted-foreground" role="status">
              Actualizado {timeFormatter.format(new Date(updatedAt))}
            </span>
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={refresh}
            disabled={isRefreshing}
            className="border-border"
          >
            <RefreshCw className={cn(isRefreshing && "animate-spin")} aria-hidden="true" />
            Actualizar
          </Button>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : error && teams.length === 0 ? (
          <div className="space-y-3">
            <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
              No se pudo cargar el ranking: {error}
            </p>
            <Button type="button" variant="outline" onClick={refresh}>
              Reintentar
            </Button>
          </div>
        ) : teams.length === 0 ? (
          <p className="py-8 text-center text-muted-foreground">Aún no hay equipos inscritos en esta maratón.</p>
        ) : (
          <>
            {error && (
              <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
                No se pudo actualizar el ranking ({error}). Se muestran los últimos datos.
              </p>
            )}
            {frozen && (
              <p className="rounded-md border border-border bg-secondary/50 px-3 py-2 text-sm text-muted-foreground">
                El ranking está congelado: los envíos de los últimos minutos no se reflejan hasta que el
                organizador publique los resultados. Tus propios veredictos siguen apareciendo en Envíos.
              </p>
            )}
            {allZero && (
              <p className="rounded-md border border-border bg-secondary/50 px-3 py-2 text-sm text-muted-foreground">
                Las posiciones se actualizarán cuando haya envíos aceptados.
              </p>
            )}

            <div className="overflow-hidden rounded-md border border-border">
              <Table>
                <TableHeader>
                  <TableRow className="border-border hover:bg-transparent">
                    <TableHead className="w-16 text-muted-foreground">Pos.</TableHead>
                    <TableHead className="min-w-[10rem] text-muted-foreground">Equipo</TableHead>
                    <TableHead className="min-w-[7rem] text-muted-foreground">Globos</TableHead>
                    <TableHead className="text-right text-muted-foreground">Puntos</TableHead>
                    <TableHead className="text-right text-muted-foreground">
                      Penalización <span className="text-xs">(min)</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {teams.map((team, index) => {
                    const position = ranks[index]
                    const mine = team.id === myTeamId
                    const freshIds = freshByTeam.get(team.id) ?? []
                    const flashColor = freshIds.length > 0 ? problemColor.get(freshIds[freshIds.length - 1]) : undefined
                    return (
                      <TableRow
                        key={team.id}
                        aria-current={mine ? "true" : undefined}
                        style={flashColor ? ({ "--flash-color": flashColor } as CSSProperties) : undefined}
                        className={cn(
                          "border-border",
                          freshIds.length > 0 && "balloon-row-flash",
                          mine ? "bg-primary/10 hover:bg-primary/15" : "hover:bg-secondary/50"
                        )}
                      >
                        <TableCell>
                          <span
                            className={cn(
                              "inline-flex h-7 min-w-7 items-center justify-center rounded-full border px-1.5 font-mono text-sm font-semibold",
                              positionTone(position)
                            )}
                          >
                            {position}
                          </span>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="break-words font-medium text-foreground">{team.teamName}</span>
                            {mine && (
                              <Badge variant="outline" className="border-primary/30 bg-primary/10 text-primary">
                                Tu equipo
                              </Badge>
                            )}
                          </div>
                          <span className="text-xs text-muted-foreground">
                            {team.members.length} {team.members.length === 1 ? "integrante" : "integrantes"}
                          </span>
                        </TableCell>
                        <TableCell>
                          <BalloonCell
                            solvedProblemIds={team.solvedProblemIds ?? []}
                            problems={problems}
                            freshIds={freshIds}
                            celebrate={mine}
                            teamName={team.teamName}
                            unknown={!mine && fetchedTeams[index]?.solvedProblemIds === undefined}
                          />
                        </TableCell>
                        <TableCell className="text-right font-mono font-semibold text-foreground">
                          {team.score}
                        </TableCell>
                        <TableCell className="text-right font-mono text-muted-foreground">
                          {team.penaltyTime}
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}
