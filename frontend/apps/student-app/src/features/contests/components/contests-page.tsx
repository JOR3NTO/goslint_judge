"use client"

import { useState } from "react"
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { ContestCard, type Contest } from "@/components/contest-card"
import { StatsCard } from "@/components/stats-card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { 
  Trophy, 
  Users, 
  Calendar, 
  Search,
  Filter,
  Zap
} from "lucide-react"

const allContests: Contest[] = [
  {
    id: "1",
    title: "Goslint Weekly Challenge #42",
    description: "Competencia semanal con problemas de estructuras de datos y algoritmos de grafos.",
    startDate: "15 Ene 2025",
    endDate: "15 Ene 2025",
    duration: "3 horas",
    participants: 234,
    difficulty: "Intermedio",
    status: "upcoming",
    problems: 6
  },
  {
    id: "2",
    title: "Maratón Nacional de Algoritmos",
    description: "La competencia más grande del año. Premios especiales para los top 10.",
    startDate: "20 Ene 2025",
    endDate: "20 Ene 2025",
    duration: "5 horas",
    participants: 1205,
    difficulty: "Avanzado",
    status: "active",
    problems: 10
  },
  {
    id: "3",
    title: "Práctica: Programación Dinámica",
    description: "Sesión de práctica enfocada en problemas clásicos de programación dinámica.",
    startDate: "18 Ene 2025",
    endDate: "18 Ene 2025",
    duration: "2 horas",
    participants: 89,
    difficulty: "Principiante",
    status: "upcoming",
    problems: 5
  },
  {
    id: "4",
    title: "Desafío de Grafos Avanzados",
    description: "Problemas complejos de teoría de grafos: flujo máximo, matching, y más.",
    startDate: "22 Ene 2025",
    endDate: "22 Ene 2025",
    duration: "4 horas",
    participants: 156,
    difficulty: "Experto",
    status: "upcoming",
    problems: 8
  },
  {
    id: "5",
    title: "Introducción a Competitive Programming",
    description: "Maratón especial para principiantes. Aprende las bases del competitive programming.",
    startDate: "25 Ene 2025",
    endDate: "25 Ene 2025",
    duration: "2 horas",
    participants: 342,
    difficulty: "Principiante",
    status: "upcoming",
    problems: 4
  },
  {
    id: "6",
    title: "Goslint Speed Coding Challenge",
    description: "Resuelve problemas fáciles lo más rápido posible. El tiempo es tu enemigo.",
    startDate: "28 Ene 2025",
    endDate: "28 Ene 2025",
    duration: "1 hora",
    participants: 189,
    difficulty: "Intermedio",
    status: "upcoming",
    problems: 10
  }
]

const difficulties = ["Todos", "Principiante", "Intermedio", "Avanzado", "Experto"]
const statuses = ["Todos", "En Curso", "Próximamente"]

export default function ContestsPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedDifficulty, setSelectedDifficulty] = useState("Todos")
  const [selectedStatus, setSelectedStatus] = useState("Todos")

  const filteredContests = allContests.filter((contest) => {
    const matchesSearch = contest.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          contest.description.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesDifficulty = selectedDifficulty === "Todos" || contest.difficulty === selectedDifficulty
    const matchesStatus = selectedStatus === "Todos" || 
                          (selectedStatus === "En Curso" && contest.status === "active") ||
                          (selectedStatus === "Próximamente" && contest.status === "upcoming")
    
    return matchesSearch && matchesDifficulty && matchesStatus
  })

  const activeContests = allContests.filter(c => c.status === "active").length
  const totalParticipants = allContests.reduce((acc, c) => acc + c.participants, 0)

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
                Maratones de <span className="text-primary text-glow">Programación</span>
              </h1>
              <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                Participa en competencias en tiempo real, resuelve problemas desafiantes y 
                demuestra tus habilidades de algoritmos.
              </p>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <StatsCard
                icon={Trophy}
                label="Maratones Activas"
                value={activeContests}
                trend="En curso ahora"
              />
              <StatsCard
                icon={Calendar}
                label="Próximas Maratones"
                value={allContests.filter(c => c.status === "upcoming").length}
              />
              <StatsCard
                icon={Users}
                label="Participantes Totales"
                value={totalParticipants.toLocaleString()}
              />
              <StatsCard
                icon={Zap}
                label="Problemas Disponibles"
                value={allContests.reduce((acc, c) => acc + c.problems, 0)}
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
              <div className="flex flex-wrap items-center gap-4">
                <div className="flex items-center gap-2">
                  <Filter className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">Filtrar:</span>
                </div>
                
                <div className="flex flex-wrap gap-2">
                  {difficulties.map((diff) => (
                    <Badge
                      key={diff}
                      variant={selectedDifficulty === diff ? "default" : "outline"}
                      className={`cursor-pointer transition-all ${
                        selectedDifficulty === diff 
                          ? "bg-primary text-primary-foreground hover:bg-primary/90" 
                          : "border-border hover:border-primary hover:bg-primary/10"
                      }`}
                      onClick={() => setSelectedDifficulty(diff)}
                    >
                      {diff}
                    </Badge>
                  ))}
                </div>

                <div className="h-6 w-px bg-border hidden sm:block" />

                <div className="flex flex-wrap gap-2">
                  {statuses.map((status) => (
                    <Badge
                      key={status}
                      variant={selectedStatus === status ? "default" : "outline"}
                      className={`cursor-pointer transition-all ${
                        selectedStatus === status 
                          ? "bg-primary text-primary-foreground hover:bg-primary/90" 
                          : "border-border hover:border-primary hover:bg-primary/10"
                      }`}
                      onClick={() => setSelectedStatus(status)}
                    >
                      {status}
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
            {filteredContests.length > 0 ? (
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
            ) : (
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
                  onClick={() => {
                    setSearchQuery("")
                    setSelectedDifficulty("Todos")
                    setSelectedStatus("Todos")
                  }}
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
