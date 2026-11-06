"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useParams, useRouter } from "next/navigation"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
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
  UPCOMING: "bg-blue-500/10 text-blue-400 border-blue-500/30",
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

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="relative py-8 border-b border-border">
          <div className="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent" />
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
                  <div className="flex items-center gap-3 flex-wrap">
                    <Badge variant="outline" className={statusColors[contest.status]}>
                      {statusLabels[contest.status]}
                    </Badge>
                  </div>
                  <h1 className="text-3xl sm:text-4xl font-bold text-foreground">
                    {contest.title}
                  </h1>
                  <p className="text-muted-foreground max-w-2xl">
                    {contest.description}
                  </p>
                  <div className="flex flex-wrap items-center gap-6 text-sm text-muted-foreground">
                    <div className="flex items-center gap-2">
                      <Calendar className="h-4 w-4 text-primary" />
                      {formatDateTime(contest.startTime)} – {formatDateTime(contest.endTime)}
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-primary" />
                      {formatDuration(contestDurationMs(contest.startTime, contest.endTime))}
                    </div>
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-primary" />
                      {sectionErrors.teams ? "—" : teams.length} participantes
                    </div>
                    <div className="flex items-center gap-2">
                      <FileCode2 className="h-4 w-4 text-primary" />
                      {sectionErrors.problems ? "—" : problems.length} problemas
                    </div>
                  </div>
                </div>

                <div className="flex flex-col gap-4 w-full lg:w-auto">
                  <ContestTimer contest={contest} />

                  {myTeam ? (
                    <Card className="border-primary/30 bg-primary/5">
                      <CardContent className="py-4 flex items-center gap-3">
                        <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0" />
                        <p className="text-foreground">
                          Inscrito como <span className="font-semibold text-primary">{myTeam.teamName}</span>
                        </p>
                      </CardContent>
                    </Card>
                  ) : contest.status !== "ENDED" ? (
                    <Button
                      onClick={openJoin}
                      className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
                    >
                      <UserPlus className="h-4 w-4" />
                      Inscribirme
                    </Button>
                  ) : null}

                  {myTeam && contest.status !== "UPCOMING" && (
                    <Link href={`/contests/${contest.id}/arena`}>
                      <Button className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm">
                        <Swords className="h-4 w-4" />
                        {contest.status === "ACTIVE" ? "Entrar al maratón" : "Ver arena"}
                      </Button>
                    </Link>
                  )}
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Mi equipo: solo si el usuario ya está inscrito en esta maratón */}
        {!isLoading && !error && contest && myTeam && (
          <section className="pt-8">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
              <MyTeamPanel
                contest={contest}
                team={myTeam}
                currentUserId={currentUserId}
                onTeamChanged={replaceMyTeam}
                onTeamLeft={clearMyTeam}
              />
            </div>
          </section>
        )}

        {/* Content */}
        {!isLoading && !error && contest && (
          <section className="py-8">
            <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
              <Tabs defaultValue="problems" className="space-y-6">
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
                        <p className="text-muted-foreground">No se pudieron cargar los problemas.</p>
                      ) : problems.length === 0 ? (
                        <p className="text-muted-foreground">Esta maratón aún no tiene problemas asignados.</p>
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
                        Equipos ordenados según el marcador del servidor
                      </CardDescription>
                    </CardHeader>
                    <CardContent>
                      {sectionErrors.scoreboard ? (
                        <p className="text-muted-foreground">No se pudo cargar el ranking.</p>
                      ) : scoreboard.length === 0 ? (
                        <p className="text-muted-foreground">Aún no hay equipos inscritos.</p>
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
        <div className="flex items-center gap-2 text-muted-foreground">
          <span
            className="inline-block h-4 w-4 rounded-full border border-border"
            style={{ backgroundColor: problem.balloonColor }}
            aria-hidden="true"
          />
          <span className="text-sm">{problem.balloonColor}</span>
        </div>
      </TableCell>
    </TableRow>
  )
}

function ScoreboardRow({ team, rank }: { team: TeamDTO; rank: number }) {
  return (
    <TableRow className="border-border hover:bg-secondary/50">
      <TableCell>
        {rank <= 3 ? (
          <div className={`flex h-8 w-8 items-center justify-center rounded-full ${
            rank === 1 ? "bg-yellow-500/20 text-yellow-400" :
            rank === 2 ? "bg-gray-400/20 text-gray-400" :
            "bg-orange-500/20 text-orange-400"
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
