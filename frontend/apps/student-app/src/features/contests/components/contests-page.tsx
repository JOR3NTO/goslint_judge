"use client"

import { useMemo, useState } from "react"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { ContestCard, ContestCardSkeleton } from "./contest-card"
import { ContestsLoadError } from "./contests-load-error"
import { useContests } from "../hooks/use-contests"
import { StatsCard } from "@/shared/ui/stats-card"
import { Button } from "@/shared/ui/primitives/button"
import { Input } from "@/shared/ui/primitives/input"
import { Badge } from "@/shared/ui/primitives/badge"
import type { ContestStatus } from "../types/contest.types"
import {
  Trophy,
  Calendar,
  Search,
  Filter,
} from "lucide-react"

/** Filtros por estado: "Todos" no filtra; el resto mapea al estado del backend. */
const STATUS_FILTERS: { label: string; value: ContestStatus | "ALL" }[] = [
  { label: "Todos", value: "ALL" },
  { label: "En Curso", value: "ACTIVE" },
  { label: "Próximamente", value: "UPCOMING" },
]

export function ContestsPage() {
  const { contests, isLoading, error, reload } = useContests()
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedStatus, setSelectedStatus] = useState<ContestStatus | "ALL">("ALL")

  const filteredContests = useMemo(() => {
    if (!contests) return []
    const query = searchQuery.trim().toLowerCase()
    return contests.filter((contest) => {
      const matchesSearch =
        query === "" ||
        contest.title.toLowerCase().includes(query) ||
        contest.description.toLowerCase().includes(query)
      const matchesStatus = selectedStatus === "ALL" || contest.status === selectedStatus
      return matchesSearch && matchesStatus
    })
  }, [contests, searchQuery, selectedStatus])

  // Estadísticas calculadas solo con datos del listado (participantes y problemas no vienen aquí).
  const activeCount = contests?.filter((c) => c.status === "ACTIVE").length ?? 0
  const upcomingCount = contests?.filter((c) => c.status === "UPCOMING").length ?? 0
  const totalCount = contests?.length ?? 0

  const clearFilters = () => {
    setSearchQuery("")
    setSelectedStatus("ALL")
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="relative py-16 border-b border-border">
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-12">
              <h1 className="text-4xl sm:text-5xl font-bold text-foreground mb-4">
                Maratones de <span className="text-primary text-glow">Programación</span>
              </h1>
              <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                Participa en competencias en tiempo real, resuelve problemas desafiantes y
                demuestra tus habilidades de algoritmos.
              </p>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <StatsCard
                icon={Trophy}
                label="Maratones Activas"
                value={isLoading ? "—" : activeCount}
                trend="En curso ahora"
              />
              <StatsCard
                icon={Calendar}
                label="Próximas Maratones"
                value={isLoading ? "—" : upcomingCount}
              />
              <StatsCard
                icon={Trophy}
                label="Maratones Registradas"
                value={isLoading ? "—" : totalCount}
              />
            </div>
          </div>
        </section>

        {/* Filters */}
        <section className="py-8 border-b border-border bg-card/30">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between">
              {/* Search */}
              <div className="relative w-full lg:w-96">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Buscar maratones..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 bg-input border-border focus:border-primary"
                />
              </div>

              {/* Filters */}
              <div className="flex items-center gap-2">
                <Filter className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">Filtrar:</span>
                <div className="flex flex-wrap gap-2">
                  {STATUS_FILTERS.map((filter) => (
                    <Badge
                      key={filter.value}
                      variant={selectedStatus === filter.value ? "default" : "outline"}
                      className={`cursor-pointer transition-all ${
                        selectedStatus === filter.value
                          ? "bg-primary text-primary-foreground hover:bg-primary/90"
                          : "border-border hover:border-primary hover:bg-primary/10"
                      }`}
                      onClick={() => setSelectedStatus(filter.value)}
                    >
                      {filter.label}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Contests Grid */}
        <section className="py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {isLoading && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 6 }, (_, i) => (
                  <ContestCardSkeleton key={i} />
                ))}
              </div>
            )}

            {!isLoading && error && <ContestsLoadError message={error} onRetry={reload} />}

            {!isLoading && !error && contests && contests.length === 0 && (
              <div className="text-center py-16">
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  Aún no hay maratones
                </h3>
                <p className="text-muted-foreground">
                  Cuando se publique una maratón aparecerá aquí.
                </p>
              </div>
            )}

            {!isLoading && !error && contests && contests.length > 0 && filteredContests.length > 0 && (
              <>
                <div className="flex items-center justify-between mb-8">
                  <p className="text-muted-foreground">
                    Mostrando <span className="text-foreground font-medium">{filteredContests.length}</span> maratones
                  </p>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filteredContests.map((contest) => (
                    <ContestCard key={contest.id} contest={contest} />
                  ))}
                </div>
              </>
            )}

            {!isLoading && !error && contests && contests.length > 0 && filteredContests.length === 0 && (
              <div className="text-center py-16">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted mx-auto mb-4">
                  <Search className="h-8 w-8 text-muted-foreground" />
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  No se encontraron maratones
                </h3>
                <p className="text-muted-foreground mb-4">
                  Intenta ajustar tus filtros de búsqueda.
                </p>
                <Button
                  variant="outline"
                  onClick={clearFilters}
                  className="border-border hover:border-primary hover:bg-primary/10"
                >
                  Limpiar Filtros
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
