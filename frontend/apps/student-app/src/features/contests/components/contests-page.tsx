"use client"

import { useMemo, useState } from "react"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { ContestCard, ContestCardSkeleton } from "./contest-card"
import { ContestsLoadError } from "./contests-load-error"
import { ContestStats } from "./contest-stats"
import { useContests } from "../hooks/use-contests"
import { Button } from "@/shared/ui/primitives/button"
import { Input } from "@/shared/ui/primitives/input"
import type { ContestListItem, ContestStatus } from "../types/contest.types"
import { Search } from "lucide-react"
import { cn } from "@/shared/utils/cn"

type StatusFilter = ContestStatus | "ALL"

/** Filtros por estado: "Todos" no filtra; el resto mapea al estado del backend. */
const STATUS_FILTERS: { label: string; value: StatusFilter }[] = [
  { label: "Todos", value: "ALL" },
  { label: "En curso", value: "ACTIVE" },
  { label: "Próximamente", value: "UPCOMING" },
  { label: "Finalizadas", value: "ENDED" },
]

/** Orden del listado: en curso primero, luego próximas y por último finalizadas. */
const STATUS_ORDER: Record<ContestStatus, number> = { ACTIVE: 0, UPCOMING: 1, ENDED: 2 }

function sortContests(list: ContestListItem[]): ContestListItem[] {
  return [...list].sort((a, b) => {
    const byStatus = STATUS_ORDER[a.status] - STATUS_ORDER[b.status]
    if (byStatus !== 0) return byStatus
    // Próximamente: la más cercana primero. Finalizadas: la más reciente primero.
    if (a.status === "ENDED") return new Date(b.endTime).getTime() - new Date(a.endTime).getTime()
    return new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
  })
}

export function ContestsPage() {
  const { contests, isLoading, error, reload } = useContests()
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedStatus, setSelectedStatus] = useState<StatusFilter>("ALL")

  const sortedContests = useMemo(() => sortContests(contests ?? []), [contests])

  const filteredContests = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    return sortedContests.filter((contest) => {
      const matchesSearch =
        query === "" ||
        contest.title.toLowerCase().includes(query) ||
        contest.description.toLowerCase().includes(query)
      const matchesStatus = selectedStatus === "ALL" || contest.status === selectedStatus
      return matchesSearch && matchesStatus
    })
  }, [sortedContests, searchQuery, selectedStatus])

  // Contadores calculados solo con datos del listado (participantes y problemas no vienen aquí).
  const counts: Record<StatusFilter, number> = {
    ALL: contests?.length ?? 0,
    ACTIVE: contests?.filter((c) => c.status === "ACTIVE").length ?? 0,
    UPCOMING: contests?.filter((c) => c.status === "UPCOMING").length ?? 0,
    ENDED: contests?.filter((c) => c.status === "ENDED").length ?? 0,
  }

  const clearFilters = () => {
    setSearchQuery("")
    setSelectedStatus("ALL")
  }

  const statValue = (n: number) => (isLoading || !contests ? "—" : n)

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="border-b border-border py-10 sm:py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mb-8 text-center">
              <h1 className="mb-3 text-3xl font-bold text-foreground sm:text-4xl">
                Maratones de <span className="text-primary text-glow">Programación</span>
              </h1>
              <p className="mx-auto max-w-2xl text-base text-muted-foreground sm:text-lg">
                Participa en competencias en tiempo real, resuelve problemas desafiantes y
                demuestra tus habilidades de algoritmos.
              </p>
            </div>

            <ContestStats
              items={[
                { label: "En curso", value: statValue(counts.ACTIVE) },
                { label: "Próximamente", value: statValue(counts.UPCOMING) },
                { label: "Total de maratones", value: statValue(counts.ALL) },
              ]}
            />
          </div>
        </section>

        {/* Filters */}
        <section className="border-b border-border bg-card/30 py-6">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="relative w-full lg:w-96">
                <Search aria-hidden="true" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="search"
                  aria-label="Buscar maratones"
                  placeholder="Buscar maratones..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>

              {/* Filtro por estado: control segmentado con contadores */}
              <div
                role="group"
                aria-label="Filtrar por estado"
                className="flex max-w-full flex-wrap gap-1 rounded-lg border border-border bg-background p-1"
              >
                {STATUS_FILTERS.map((filter) => {
                  const active = selectedStatus === filter.value
                  return (
                    <button
                      key={filter.value}
                      type="button"
                      aria-pressed={active}
                      onClick={() => setSelectedStatus(filter.value)}
                      className={cn(
                        "inline-flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                        active
                          ? "bg-primary text-primary-foreground"
                          : "text-muted-foreground hover:bg-accent hover:text-foreground"
                      )}
                    >
                      {filter.label}
                      <span
                        className={cn(
                          "rounded px-1.5 text-xs tabular-nums",
                          active ? "bg-primary-foreground/20" : "bg-muted text-muted-foreground"
                        )}
                      >
                        {statValue(counts[filter.value])}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          </div>
        </section>

        {/* Contests Grid */}
        <section className="py-10 sm:py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {isLoading && (
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                {Array.from({ length: 6 }, (_, i) => (
                  <ContestCardSkeleton key={i} />
                ))}
              </div>
            )}

            {!isLoading && error && <ContestsLoadError message={error} onRetry={reload} />}

            {!isLoading && !error && contests && contests.length === 0 && (
              <div className="py-16 text-center">
                <h3 className="mb-2 text-lg font-semibold text-foreground">
                  Aún no hay maratones
                </h3>
                <p className="text-muted-foreground">
                  Cuando se publique una maratón aparecerá aquí.
                </p>
              </div>
            )}

            {!isLoading && !error && contests && contests.length > 0 && filteredContests.length > 0 && (
              <>
                <p className="mb-6 text-sm text-muted-foreground" aria-live="polite">
                  Mostrando <span className="font-medium text-foreground tabular-nums">{filteredContests.length}</span>{" "}
                  {filteredContests.length === 1 ? "maratón" : "maratones"}
                </p>
                <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                  {filteredContests.map((contest) => (
                    <ContestCard key={contest.id} contest={contest} />
                  ))}
                </div>
              </>
            )}

            {!isLoading && !error && contests && contests.length > 0 && filteredContests.length === 0 && (
              <div className="py-16 text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-muted">
                  <Search aria-hidden="true" className="h-8 w-8 text-muted-foreground" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-foreground">
                  No se encontraron maratones
                </h3>
                <p className="mb-4 text-muted-foreground">
                  Intenta ajustar tus filtros de búsqueda.
                </p>
                <Button variant="outline" onClick={clearFilters}>
                  Limpiar filtros
                </Button>
              </div>
            )}
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
