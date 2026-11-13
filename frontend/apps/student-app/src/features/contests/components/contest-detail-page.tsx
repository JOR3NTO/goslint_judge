"use client"

import { useEffect, useState, type ReactNode } from "react"
import Link from "next/link"
import { useParams, useRouter } from "next/navigation"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { cn } from "@/shared/utils/cn"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/primitives/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/primitives/table"
import {
  ArrowLeft,
  Trophy,
  Clock,
  Users,
  Calendar,
  FileCode2,
  Medal,
  CheckCircle2,
  UserPlus,
  Swords,
} from "lucide-react"
import { useContestDetail } from "../hooks/use-contest-detail"
import type { ContestStatus, ContestProblemItem, TeamDTO } from "../types/contest.types"
import { formatDateTime, contestDurationMs, formatDuration } from "../utils/format"
import { readContestSession } from "../utils/session"
import { ContestTimer } from "./contest-timer"
import { ContestsLoadError } from "./contests-load-error"
import { JoinContestDialog } from "./join-contest-dialog"
import { MyTeamPanel } from "./my-team-panel"

const statusColors: Record<ContestStatus, string> = {
  UPCOMING: "bg-info/10 text-info border-info/30",
  ACTIVE: "bg-primary/10 text-primary border-primary/30",
  ENDED: "bg-muted text-muted-foreground border-muted",
}

const statusLabels: Record<ContestStatus, string> = {
  UPCOMING: "Próximamente",
  ACTIVE: "En Curso",
  ENDED: "Finalizado",
}

export function ContestDetailPage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const {
    contest,
    problems,
    scoreboard,
    teams,
    myTeam,
    error,
    sectionErrors,
    isLoading,
    reload,
    joinContest,
    replaceMyTeam,
    clearMyTeam,
  } = useContestDetail(id)
  const [isJoinOpen, setIsJoinOpen] = useState(false)
  // Id del usuario autenticado; se lee en el cliente tras montar (no hay localStorage en SSR).
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)
  useEffect(() => {
    setCurrentUserId(readContestSession()?.userId ?? null)
  }, [])

  const openJoin = () => {
    // Sin sesión no se puede inscribir: se manda al login.
    if (!readContestSession()) {
      router.push("/login")
      return
    }
    setIsJoinOpen(true)
  }

  // Acción principal del hero: inscribirse si no hay equipo, o entrar a la arena si ya está inscrito.
  let primaryAction: ReactNode = null
  if (contest && !isLoading && !error) {
    if (!myTeam && contest.status !== "ENDED") {
      primaryAction = (
        <Button
          size="lg"
          onClick={openJoin}
          className="h-12 w-full gap-2 bg-primary text-base text-primary-foreground shadow-sm hover:bg-primary/90"
        >
          <UserPlus aria-hidden="true" className="h-5 w-5" />
          Inscribirme
        </Button>
      )
    } else if (myTeam && contest.status !== "UPCOMING") {
      primaryAction = (
        <Button
          asChild
          size="lg"
          className="h-12 w-full gap-2 bg-primary text-base text-primary-foreground shadow-sm hover:bg-primary/90 glow-green-sm"
        >
          <Link href={`/contests/${contest.id}/arena`}>
            <Swords aria-hidden="true" className="h-5 w-5" />
            {contest.status === "ACTIVE" ? "Entrar al maratón" : "Ver arena"}
          </Link>
        </Button>
      )
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="relative py-8 border-b border-border">
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <Link
              href="/contests"
              className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6 transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
              Volver a Maratones
            </Link>

            {isLoading && <ContestHeaderSkeleton />}

            {!isLoading && error && <ContestsLoadError message={error} onRetry={reload} />}

            {!isLoading && !error && contest && (
              <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
                <div className="space-y-4 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Badge variant="outline" className={statusColors[contest.status]}>
                      {statusLabels[contest.status]}
                    </Badge>
                    {myTeam && (
                      <Badge variant="outline" className="gap-1.5 border-primary/30 bg-primary/10 px-2.5 py-1 text-sm text-primary">
                        <CheckCircle2 aria-hidden="true" />
                        Inscrito como <span className="font-semibold">{myTeam.teamName}</span>
                      </Badge>
                    )}
                  </div>
                  <h1 className="text-3xl sm:text-4xl font-bold text-foreground break-words">
                    {contest.title}
                  </h1>
                  <p className="text-muted-foreground max-w-2xl">
                    {contest.description}
                  </p>
                  <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-muted-foreground">
                    <div className="flex items-center gap-2">
                      <Calendar aria-hidden="true" className="h-4 w-4 text-primary" />
                      {formatDateTime(contest.startTime)} – {formatDateTime(contest.endTime)}
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock aria-hidden="true" className="h-4 w-4 text-primary" />
                      {formatDuration(contestDurationMs(contest.startTime, contest.endTime))}
                    </div>
                    <div className="flex items-center gap-2">
                      <Users aria-hidden="true" className="h-4 w-4 text-primary" />
                      {sectionErrors.teams ? "—" : teams.length} participantes
                    </div>
                    <div className="flex items-center gap-2">
                      <FileCode2 aria-hidden="true" className="h-4 w-4 text-primary" />
                      {sectionErrors.problems ? "—" : problems.length} problemas
                    </div>
                  </div>
                </div>

                <div className="flex flex-col gap-4 w-full lg:w-80 lg:flex-shrink-0">
                  <ContestTimer contest={contest} />

                  {primaryAction}
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Contenido: problemas y ranking; el panel del equipo va en columna lateral en escritorio */}
        {!isLoading && !error && contest && (
          <section className="py-8">
            <div
              className={cn(
                "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 grid gap-8",
                myTeam && "lg:grid-cols-[minmax(0,1fr)_360px] lg:items-start"
              )}
            >
              <Tabs defaultValue="problems" className="min-w-0 space-y-6">
                <TabsList className="bg-secondary/50 border border-border">
                  <TabsTrigger value="problems" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
                    <FileCode2 className="h-4 w-4 mr-2" />
                    Problemas
                  </TabsTrigger>
                  <TabsTrigger value="leaderboard" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
                    <Trophy className="h-4 w-4 mr-2" />
                    Ranking
                  </TabsTrigger>
                </TabsList>

                {/* Problems Tab (solo listado: la vista del problema llega en la Fase 3) */}
                <TabsContent value="problems">
                  <Card className="border-border bg-card">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <FileCode2 className="h-5 w-5 text-primary" />
                        Problemas ({sectionErrors.problems ? "—" : problems.length})
                      </CardTitle>
                      <CardDescription>
                        Problemas asignados a esta maratón
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      {sectionErrors.problems ? (
                        <EmptyState
                          title="No se pudieron cargar los problemas"
                          description="Revisa tu conexión e inténtalo de nuevo más tarde."
                        />
                      ) : problems.length === 0 ? (
                        <EmptyState
                          title="Aún no hay problemas"
                          description="Esta maratón todavía no tiene problemas asignados. Vuelve a revisar más tarde."
                        />
                      ) : (
                        <div className="overflow-x-auto">
                          <Table>
                            <TableHeader>
                              <TableRow className="border-border hover:bg-transparent">
                                <TableHead className="text-muted-foreground w-16">#</TableHead>
                                <TableHead className="text-muted-foreground">Problema</TableHead>
                                <TableHead className="text-muted-foreground text-center">Puntos</TableHead>
                                <TableHead className="text-muted-foreground text-center">Penalización</TableHead>
                                <TableHead className="text-muted-foreground">Globo</TableHead>
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              {problems.map((problem) => (
                                <ProblemRow key={problem.id} problem={problem} />
                              ))}
                            </TableBody>
                          </Table>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>

                {/* Leaderboard Tab */}
                <TabsContent value="leaderboard">
                  <Card className="border-border bg-card">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        <Trophy className="h-5 w-5 text-primary" />
                        Tabla de Posiciones
                      </CardTitle>
                      <CardDescription>
                        {contest?.scoreboardStatus === "FROZEN"
                          ? "Ranking congelado: los envíos de los últimos minutos se publicarán cuando el organizador libere los resultados."
                          : "Equipos ordenados según el marcador del servidor"}
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      {sectionErrors.scoreboard ? (
                        <EmptyState
                          title="No se pudo cargar el ranking"
                          description="Inténtalo de nuevo más tarde."
                        />
                      ) : scoreboard.length === 0 ? (
                        <EmptyState
                          title="Aún no hay equipos inscritos"
                          description="El ranking aparecerá cuando se inscriba el primer equipo."
                        />
                      ) : (
                        <div className="overflow-x-auto">
                          <Table>
                            <TableHeader>
                              <TableRow className="border-border hover:bg-transparent">
                                <TableHead className="text-muted-foreground w-16">Rank</TableHead>
                                <TableHead className="text-muted-foreground">Equipo</TableHead>
                                <TableHead className="text-muted-foreground text-center">Puntaje</TableHead>
                                <TableHead className="text-muted-foreground text-center">Penalización</TableHead>
                                <TableHead className="text-muted-foreground text-right">Miembros</TableHead>
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              {scoreboard.map((team, index) => (
                                <ScoreboardRow key={team.id} team={team} rank={index + 1} />
                              ))}
                            </TableBody>
                          </Table>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                </TabsContent>
              </Tabs>

              {/* Mi equipo: solo si el usuario ya está inscrito en esta maratón */}
              {myTeam && (
                <div className="min-w-0">
                  <MyTeamPanel
                    contest={contest}
                    team={myTeam}
                    currentUserId={currentUserId}
                    onTeamChanged={replaceMyTeam}
                    onTeamLeft={clearMyTeam}
                  />
                </div>
              )}
            </div>
          </section>
        )}

        {contest && (
          <JoinContestDialog
            open={isJoinOpen}
            onOpenChange={setIsJoinOpen}
            contestTitle={contest.title}
            onJoin={joinContest}
          />
        )}
      </main>
      <Footer />
    </div>
  )
}

function ProblemRow({ problem }: { problem: ContestProblemItem }) {
  return (
    <TableRow className="border-border hover:bg-secondary/50">
      <TableCell>
        <span className="flex h-8 w-8 items-center justify-center rounded bg-primary/10 text-primary font-mono font-bold">
          {problem.alias}
        </span>
      </TableCell>
      <TableCell>
        <p className="font-medium text-foreground">
          {problem.title ?? <span className="text-muted-foreground">Problema {problem.alias}</span>}
        </p>
      </TableCell>
      <TableCell className="text-center font-mono text-primary">{problem.points}</TableCell>
      <TableCell className="text-center font-mono text-muted-foreground">{problem.penalty}</TableCell>
      <TableCell>
        <span
          role="img"
          aria-label={`Globo ${balloonColorLabel(problem.balloonColor)}`}
          title={balloonColorLabel(problem.balloonColor)}
          className="inline-block h-4 w-4 rounded-full border border-border"
          style={{ backgroundColor: problem.balloonColor }}
        />
      </TableCell>
    </TableRow>
  )
}

/** Nombres en español de los colores de globo más comunes; el resto se muestra tal cual. */
const BALLOON_COLOR_NAMES: Record<string, string> = {
  red: "rojo",
  blue: "azul",
  green: "verde",
  yellow: "amarillo",
  orange: "naranja",
  purple: "morado",
  violet: "violeta",
  pink: "rosa",
  black: "negro",
  white: "blanco",
  brown: "café",
  gray: "gris",
  grey: "gris",
  cyan: "cian",
  teal: "verde azulado",
}

function balloonColorLabel(color: string): string {
  const key = color.trim().toLowerCase()
  if (BALLOON_COLOR_NAMES[key]) return BALLOON_COLOR_NAMES[key]
  return key.startsWith("#") ? "personalizado" : color
}

/** Estado vacío con título y explicación, para las pestañas. */
function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="py-10 text-center">
      <p className="font-medium text-foreground">{title}</p>
      <p className="mt-1 text-sm text-muted-foreground">{description}</p>
    </div>
  )
}

function ScoreboardRow({ team, rank }: { team: TeamDTO; rank: number }) {
  return (
    <TableRow className="border-border hover:bg-secondary/50">
      <TableCell>
        {rank <= 3 ? (
          <div className={`flex h-8 w-8 items-center justify-center rounded-full ${
            rank === 1 ? "bg-warning/15 text-warning" :
            rank === 2 ? "bg-muted text-muted-foreground" :
            "bg-info/10 text-info"
          }`}>
            <Medal className="h-4 w-4" />
          </div>
        ) : (
          <span className="text-muted-foreground font-mono">{rank}</span>
        )}
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 border border-primary/30 text-primary font-medium">
            {team.teamName.charAt(0).toUpperCase()}
          </div>
          <span className="font-medium text-foreground">{team.teamName}</span>
        </div>
      </TableCell>
      <TableCell className="text-center font-bold text-primary">{team.score}</TableCell>
      <TableCell className="text-center font-mono text-muted-foreground">{team.penaltyTime}</TableCell>
      <TableCell className="text-right text-muted-foreground">{team.members.length}</TableCell>
    </TableRow>
  )
}

function ContestHeaderSkeleton() {
  return (
    <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
      <div className="space-y-4 flex-1">
        <Skeleton className="h-6 w-28" />
        <Skeleton className="h-10 w-3/4" />
        <Skeleton className="h-4 w-full max-w-2xl" />
        <Skeleton className="h-4 w-2/3" />
      </div>
      <Skeleton className="h-56 w-full lg:w-72" />
    </div>
  )
}
