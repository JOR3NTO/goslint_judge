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
import { StatsCard } from "@/shared/ui/stats-card"
import {
  Trophy,
  Users,
  Calendar,
  Search,
  ExternalLink,
  Clock,
  ChevronLeft,
  ChevronRight
} from "lucide-react"
import { useContests } from "../hooks/use-contests"
import { useContestCounts } from "../hooks/use-contest-counts"
import type { ContestListItem } from "../types/contest.types"
import { formatDate, formatDuration } from "../utils/format"
import { ContestsLoadError } from "./contests-load-error"

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

  const latestEnded = endedContests[0]

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="relative py-16 border-b border-border">
          <div className="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent" />
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-12">
              <h1 className="text-4xl sm:text-5xl font-bold text-foreground mb-4">
                Maratones <span className="text-primary text-glow">Anteriores</span>
              </h1>
              <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                Explora el historial de competencias y revisa los resultados de maratones pasadas.
              </p>
            </div>

            {/* Quick Stats (solo las calculables con los datos reales del listado) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <StatsCard
                icon={Trophy}
                label="Maratones Finalizadas"
                value={isLoading ? "—" : endedContests.length}
              />
              <StatsCard
                icon={Calendar}
                label="Última Finalizada"
                value={isLoading ? "—" : latestEnded ? formatDate(latestEnded.endTime) : "—"}
              />
            </div>
          </div>
        </section>

        {/* Table Section */}
        <section className="py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <Card className="border-border bg-card">
              <CardHeader className="border-b border-border">
                <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
                  <h2 className="text-xl font-semibold text-foreground">
                    Historial de Maratones
                  </h2>
                  <div className="relative w-full sm:w-72">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      placeholder="Buscar maratón..."
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value)
                        setCurrentPage(1)
                      }}
                      className="pl-10 bg-input border-border focus:border-primary"
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
                  <div className="text-center py-16 px-6">
                    <h3 className="text-lg font-semibold text-foreground mb-2">
                      Aún no hay maratones finalizadas
                    </h3>
                    <p className="text-muted-foreground">
                      Aquí aparecerán las maratones cuando terminen.
                    </p>
                  </div>
                )}

                {!isLoading && !error && endedContests.length > 0 && filteredContests.length === 0 && (
                  <div className="text-center py-16 px-6">
                    <h3 className="text-lg font-semibold text-foreground mb-2">
                      No se encontraron maratones
                    </h3>
                    <p className="text-muted-foreground">
                      Intenta con otro nombre.
                    </p>
                  </div>
                )}

                {!isLoading && !error && paginatedContests.length > 0 && (
                  <>
                    <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow className="border-border hover:bg-transparent">
                            <TableHead className="text-muted-foreground">Maratón</TableHead>
                            <TableHead className="text-muted-foreground">Fecha</TableHead>
                            <TableHead className="text-muted-foreground">Participantes</TableHead>
                            <TableHead className="text-muted-foreground text-right">Acciones</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {paginatedContests.map((contest) => (
                            <PastContestRow key={contest.id} contest={contest} />
                          ))}
                        </TableBody>
                      </Table>
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                      <div className="flex items-center justify-between px-6 py-4 border-t border-border">
                        <p className="text-sm text-muted-foreground">
                          Mostrando {(safePage - 1) * ITEMS_PER_PAGE + 1} - {Math.min(safePage * ITEMS_PER_PAGE, filteredContests.length)} de {filteredContests.length}
                        </p>
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setCurrentPage(Math.max(1, safePage - 1))}
                            disabled={safePage === 1}
                            className="border-border hover:border-primary hover:bg-primary/10"
                          >
                            <ChevronLeft className="h-4 w-4" />
                          </Button>
                          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                            <Button
                              key={page}
                              variant={safePage === page ? "default" : "outline"}
                              size="sm"
                              onClick={() => setCurrentPage(page)}
                              className={safePage === page
                                ? "bg-primary text-primary-foreground"
                                : "border-border hover:border-primary hover:bg-primary/10"
                              }
                            >
                              {page}
                            </Button>
                          ))}
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setCurrentPage(Math.min(totalPages, safePage + 1))}
                            disabled={safePage === totalPages}
                            className="border-border hover:border-primary hover:bg-primary/10"
                          >
                            <ChevronRight className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
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

/** Fila de la tabla: los participantes se piden por maratón (puede mostrar "—"). */
function PastContestRow({ contest }: { contest: ContestListItem }) {
  const { participants, isLoading } = useContestCounts(contest.id)

  return (
    <TableRow className="border-border hover:bg-secondary/50">
      <TableCell>
        <div>
          <p className="font-medium text-foreground">{contest.title}</p>
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatDuration(contest.durationMs)}
          </p>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2 text-muted-foreground">
          <Calendar className="h-4 w-4" />
          {formatDate(contest.startTime)}
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-2 text-muted-foreground">
          <Users className="h-4 w-4" />
          {isLoading ? <Skeleton className="h-4 w-10" /> : participants ?? "—"}
        </div>
      </TableCell>
      <TableCell className="text-right">
        <Link href={`/contests/${contest.id}`}>
          <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground hover:text-foreground">
            Ver Resultados
            <ExternalLink className="h-4 w-4" />
          </Button>
        </Link>
      </TableCell>
    </TableRow>
  )
}
