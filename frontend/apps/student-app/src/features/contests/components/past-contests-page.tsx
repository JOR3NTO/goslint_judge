"use client"

import { useMemo, useState } from "react"
import Link from "next/link"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardHeader } from "@/shared/ui/primitives/card"
import { Input } from "@/shared/ui/primitives/input"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/primitives/table"
import { Calendar, ChevronLeft, ChevronRight, Clock, Search, Trophy, Users } from "lucide-react"
import { useContests } from "../hooks/use-contests"
import { useParticipantCounts } from "../hooks/use-participant-counts"
import type { ContestListItem } from "../types/contest.types"
import { formatDate, formatDuration } from "../utils/format"
import { ContestsLoadError } from "./contests-load-error"
import { ContestStats } from "./contest-stats"

const ITEMS_PER_PAGE = 5

export function PastContestsPage() {
  const { contests, isLoading, error, reload } = useContests()
  const [searchQuery, setSearchQuery] = useState("")
  const [currentPage, setCurrentPage] = useState(1)

  // Solo maratones finalizadas, la más reciente primero.
  const endedContests = useMemo(
    () =>
      (contests ?? [])
        .filter((c) => c.status === "ENDED")
        .sort((a, b) => new Date(b.endTime).getTime() - new Date(a.endTime).getTime()),
    [contests]
  )

  const filteredContests = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    return endedContests.filter((contest) => contest.title.toLowerCase().includes(query))
  }, [endedContests, searchQuery])

  const totalPages = Math.max(1, Math.ceil(filteredContests.length / ITEMS_PER_PAGE))
  const safePage = Math.min(currentPage, totalPages)
  const paginatedContests = filteredContests.slice(
    (safePage - 1) * ITEMS_PER_PAGE,
    safePage * ITEMS_PER_PAGE
  )

  // Participantes solo de la página visible (una petición por maratón, compartida por tabla y tarjetas).
  const { counts: participantCounts, isLoading: countsLoading } = useParticipantCounts(
    paginatedContests.map((c) => c.id)
  )

  const latestEnded = endedContests[0]
  const statValue = (value: string | number) => (isLoading ? "—" : value)

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="border-b border-border py-10 sm:py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mb-8 text-center">
              <h1 className="mb-3 text-3xl font-bold text-foreground sm:text-4xl">
                Maratones <span className="text-primary text-glow">Anteriores</span>
              </h1>
              <p className="mx-auto max-w-2xl text-base text-muted-foreground sm:text-lg">
                Explora el historial de competencias y revisa los resultados de maratones pasadas.
              </p>
            </div>

            {/* Métricas compactas (solo las calculables con los datos reales del listado) */}
            <ContestStats
              items={[
                { label: "Maratones finalizadas", value: statValue(endedContests.length) },
                {
                  label: "Última finalizada",
                  value: statValue(latestEnded ? formatDate(latestEnded.endTime) : "—"),
                },
              ]}
            />
          </div>
        </section>

        {/* Table Section */}
        <section className="py-10 sm:py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <Card className="border-border bg-card gap-0 py-0">
              <CardHeader className="border-b border-border py-5">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <h2 className="text-xl font-semibold text-foreground">Historial de maratones</h2>
                  <div className="relative w-full sm:w-72">
                    <Search aria-hidden="true" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      type="search"
                      aria-label="Buscar maratón por nombre"
                      placeholder="Buscar maratón..."
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value)
                        setCurrentPage(1)
                      }}
                      className="pl-10"
                    />
                  </div>
                </div>
              </CardHeader>
              <CardContent className="p-0">
                {isLoading && (
                  <div className="space-y-3 p-6">
                    {Array.from({ length: 5 }, (_, i) => (
                      <Skeleton key={i} className="h-12 w-full" />
                    ))}
                  </div>
                )}

                {!isLoading && error && (
                  <div className="p-6">
                    <ContestsLoadError message={error} onRetry={reload} />
                  </div>
                )}

                {!isLoading && !error && endedContests.length === 0 && (
                  <div className="px-6 py-16 text-center">
                    <h3 className="mb-2 text-lg font-semibold text-foreground">
                      Aún no hay maratones finalizadas
                    </h3>
                    <p className="text-muted-foreground">
                      Aquí aparecerán las maratones cuando terminen.
                    </p>
                  </div>
                )}

                {!isLoading && !error && endedContests.length > 0 && filteredContests.length === 0 && (
                  <div className="px-6 py-16 text-center">
                    <h3 className="mb-2 text-lg font-semibold text-foreground">
                      No se encontraron maratones
                    </h3>
                    <p className="text-muted-foreground">
                      Intenta con otro nombre.
                    </p>
                  </div>
                )}

                {!isLoading && !error && paginatedContests.length > 0 && (
                  <>
                    {/* Escritorio: tabla */}
                    <div className="hidden overflow-x-auto md:block">
                      <Table>
                        <TableHeader>
                          <TableRow className="border-border hover:bg-transparent">
                            <TableHead className="text-muted-foreground">Maratón</TableHead>
                            <TableHead className="text-muted-foreground">Fecha</TableHead>
                            <TableHead className="text-muted-foreground">Participantes</TableHead>
                            <TableHead className="text-right text-muted-foreground">Resultados</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {paginatedContests.map((contest) => (
                            <PastContestRow
                              key={contest.id}
                              contest={contest}
                              participants={participantCounts[contest.id]}
                              participantsLoading={countsLoading && !(contest.id in participantCounts)}
                            />
                          ))}
                        </TableBody>
                      </Table>
                    </div>

                    {/* Móvil: tarjetas apiladas */}
                    <ul className="flex flex-col gap-3 p-4 md:hidden">
                      {paginatedContests.map((contest) => (
                        <PastContestCard
                          key={contest.id}
                          contest={contest}
                          participants={participantCounts[contest.id]}
                          participantsLoading={countsLoading && !(contest.id in participantCounts)}
                        />
                      ))}
                    </ul>

                    {/* Pagination */}
                    {totalPages > 1 && (
                      <nav
                        aria-label="Paginación del historial"
                        className="flex flex-col gap-3 border-t border-border px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6"
                      >
                        <p className="text-sm text-muted-foreground">
                          Mostrando {(safePage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(safePage * ITEMS_PER_PAGE, filteredContests.length)} de {filteredContests.length}
                        </p>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            aria-label="Página anterior"
                            onClick={() => setCurrentPage(Math.max(1, safePage - 1))}
                            disabled={safePage === 1}
                          >
                            <ChevronLeft aria-hidden="true" className="h-4 w-4" />
                          </Button>
                          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                            <Button
                              key={page}
                              variant={safePage === page ? "default" : "outline"}
                              size="sm"
                              aria-label={`Página ${page}`}
                              aria-current={safePage === page ? "page" : undefined}
                              onClick={() => setCurrentPage(page)}
                              className="min-w-8 tabular-nums"
                            >
                              {page}
                            </Button>
                          ))}
                          <Button
                            variant="outline"
                            size="sm"
                            aria-label="Página siguiente"
                            onClick={() => setCurrentPage(Math.min(totalPages, safePage + 1))}
                            disabled={safePage === totalPages}
                          >
                            <ChevronRight aria-hidden="true" className="h-4 w-4" />
                          </Button>
                        </div>
                      </nav>
                    )}
                  </>
                )}
              </CardContent>
            </Card>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}

interface PastContestEntryProps {
  contest: ContestListItem
  participants: number | null | undefined
  participantsLoading: boolean
}

/** Texto del número de participantes: skeleton mientras carga, "—" si no se pudo obtener. */
function ParticipantsValue({ participants, loading }: { participants: number | null | undefined; loading: boolean }) {
  if (loading) return <Skeleton className="h-4 w-10" />
  return <span className="tabular-nums">{participants ?? "—"}</span>
}

/** Fila de la tabla (escritorio). */
function PastContestRow({ contest, participants, participantsLoading }: PastContestEntryProps) {
  return (
    <TableRow className="border-border hover:bg-secondary/50">
      <TableCell className="whitespace-normal">
        <div className="min-w-0">
          <p title={contest.title} className="line-clamp-2 font-medium text-foreground">
            {contest.title}
          </p>
          <p className="flex items-center gap-1 text-xs text-muted-foreground">
            <Clock aria-hidden="true" className="h-3 w-3" />
            {formatDuration(contest.durationMs)}
          </p>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2 text-muted-foreground">
          <Calendar aria-hidden="true" className="h-4 w-4" />
          {formatDate(contest.startTime)}
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2 text-muted-foreground">
          <Users aria-hidden="true" className="h-4 w-4" />
          <ParticipantsValue participants={participants} loading={participantsLoading} />
        </div>
      </TableCell>
      <TableCell className="text-right">
        <Button asChild variant="outline" size="sm">
          <Link href={`/contests/${contest.id}`}>
            <Trophy aria-hidden="true" className="h-4 w-4" />
            Ver resultados
            <span className="sr-only">({contest.title})</span>
          </Link>
        </Button>
      </TableCell>
    </TableRow>
  )
}

/** Tarjeta apilada (móvil), con la misma información que la fila de la tabla. */
function PastContestCard({ contest, participants, participantsLoading }: PastContestEntryProps) {
  return (
    <li className="rounded-lg border border-border bg-background p-4">
      <p title={contest.title} className="line-clamp-2 font-medium text-foreground">
        {contest.title}
      </p>
      <div className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-sm text-muted-foreground">
        <span className="flex items-center gap-2">
          <Calendar aria-hidden="true" className="h-4 w-4 flex-shrink-0" />
          {formatDate(contest.startTime)}
        </span>
        <span className="flex items-center gap-2">
          <Clock aria-hidden="true" className="h-4 w-4 flex-shrink-0" />
          {formatDuration(contest.durationMs)}
        </span>
        <span className="col-span-2 flex items-center gap-2">
          <Users aria-hidden="true" className="h-4 w-4 flex-shrink-0" />
          <ParticipantsValue participants={participants} loading={participantsLoading} />
          <span>participantes</span>
        </span>
      </div>
      <Button asChild variant="outline" size="sm" className="mt-4 w-full">
        <Link href={`/contests/${contest.id}`}>
          <Trophy aria-hidden="true" className="h-4 w-4" />
          Ver resultados
          <span className="sr-only">({contest.title})</span>
        </Link>
      </Button>
    </li>
  )
}
