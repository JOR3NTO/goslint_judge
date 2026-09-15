"use client"

import { useState } from "react"
import Link from "next/link"
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { 
  Trophy, 
  Users, 
  Calendar, 
  Search,
  ExternalLink,
  Medal,
  Clock,
  ChevronLeft,
  ChevronRight
} from "lucide-react"

interface PastContest {
  id: string
  title: string
  date: string
  participants: number
  difficulty: "Principiante" | "Intermedio" | "Avanzado" | "Experto"
  winner: {
    name: string
    avatar: string
  }
  problems: number
  duration: string
}

const pastContests: PastContest[] = [
  {
    id: "p1",
    title: "Goslint Weekly Challenge #41",
    date: "8 Ene 2025",
    participants: 312,
    difficulty: "Intermedio",
    winner: { name: "CodeMaster42", avatar: "C" },
    problems: 6,
    duration: "3 horas"
  },
  {
    id: "p2",
    title: "Maratón Navideña 2024",
    date: "25 Dic 2024",
    participants: 1543,
    difficulty: "Avanzado",
    winner: { name: "AlgoNinja", avatar: "A" },
    problems: 12,
    duration: "5 horas"
  },
  {
    id: "p3",
    title: "Goslint Weekly Challenge #40",
    date: "1 Dic 2024",
    participants: 289,
    difficulty: "Intermedio",
    winner: { name: "ByteRunner", avatar: "B" },
    problems: 6,
    duration: "3 horas"
  },
  {
    id: "p4",
    title: "Desafío Universitario 2024",
    date: "15 Nov 2024",
    participants: 2100,
    difficulty: "Experto",
    winner: { name: "GraphTheorist", avatar: "G" },
    problems: 10,
    duration: "5 horas"
  },
  {
    id: "p5",
    title: "Hackathon de Algoritmos",
    date: "1 Nov 2024",
    participants: 456,
    difficulty: "Avanzado",
    winner: { name: "DPMaster", avatar: "D" },
    problems: 8,
    duration: "4 horas"
  },
  {
    id: "p6",
    title: "Práctica: Divide y Vencerás",
    date: "20 Oct 2024",
    participants: 178,
    difficulty: "Principiante",
    winner: { name: "NewCoder", avatar: "N" },
    problems: 5,
    duration: "2 horas"
  },
  {
    id: "p7",
    title: "Goslint Weekly Challenge #39",
    date: "15 Oct 2024",
    participants: 276,
    difficulty: "Intermedio",
    winner: { name: "FastSolver", avatar: "F" },
    problems: 6,
    duration: "3 horas"
  },
  {
    id: "p8",
    title: "Copa Regional de Programación",
    date: "1 Oct 2024",
    participants: 890,
    difficulty: "Avanzado",
    winner: { name: "AlgoKing", avatar: "A" },
    problems: 9,
    duration: "4 horas"
  }
]

const difficultyColors = {
  Principiante: "bg-green-500/10 text-green-400 border-green-500/30",
  Intermedio: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  Avanzado: "bg-orange-500/10 text-orange-400 border-orange-500/30",
  Experto: "bg-red-500/10 text-red-400 border-red-500/30",
}

export default function PastContestsPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 5

  const filteredContests = pastContests.filter((contest) =>
    contest.title.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const totalPages = Math.ceil(filteredContests.length / itemsPerPage)
  const paginatedContests = filteredContests.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  )

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
                Explora el historial de competencias, revisa los resultados y 
                practica con problemas de maratones pasadas.
              </p>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <Card className="border-border bg-card/50 text-center">
                <CardContent className="pt-6">
                  <Trophy className="h-8 w-8 text-primary mx-auto mb-2" />
                  <p className="text-2xl font-bold text-foreground">{pastContests.length}</p>
                  <p className="text-sm text-muted-foreground">Maratones Finalizadas</p>
                </CardContent>
              </Card>
              <Card className="border-border bg-card/50 text-center">
                <CardContent className="pt-6">
                  <Users className="h-8 w-8 text-primary mx-auto mb-2" />
                  <p className="text-2xl font-bold text-foreground">
                    {pastContests.reduce((acc, c) => acc + c.participants, 0).toLocaleString()}
                  </p>
                  <p className="text-sm text-muted-foreground">Participaciones Totales</p>
                </CardContent>
              </Card>
              <Card className="border-border bg-card/50 text-center">
                <CardContent className="pt-6">
                  <Medal className="h-8 w-8 text-primary mx-auto mb-2" />
                  <p className="text-2xl font-bold text-foreground">
                    {new Set(pastContests.map(c => c.winner.name)).size}
                  </p>
                  <p className="text-sm text-muted-foreground">Ganadores Únicos</p>
                </CardContent>
              </Card>
              <Card className="border-border bg-card/50 text-center">
                <CardContent className="pt-6">
                  <Clock className="h-8 w-8 text-primary mx-auto mb-2" />
                  <p className="text-2xl font-bold text-foreground">
                    {pastContests.reduce((acc, c) => acc + c.problems, 0)}
                  </p>
                  <p className="text-sm text-muted-foreground">Problemas Resueltos</p>
                </CardContent>
              </Card>
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
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow className="border-border hover:bg-transparent">
                        <TableHead className="text-muted-foreground">Maratón</TableHead>
                        <TableHead className="text-muted-foreground">Fecha</TableHead>
                        <TableHead className="text-muted-foreground">Dificultad</TableHead>
                        <TableHead className="text-muted-foreground">Participantes</TableHead>
                        <TableHead className="text-muted-foreground">Ganador</TableHead>
                        <TableHead className="text-muted-foreground text-right">Acciones</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {paginatedContests.map((contest) => (
                        <TableRow key={contest.id} className="border-border hover:bg-secondary/50">
                          <TableCell>
                            <div>
                              <p className="font-medium text-foreground">{contest.title}</p>
                              <p className="text-xs text-muted-foreground">
                                {contest.problems} problemas · {contest.duration}
                              </p>
                            </div>
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2 text-muted-foreground">
                              <Calendar className="h-4 w-4" />
                              {contest.date}
                            </div>
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className={difficultyColors[contest.difficulty]}>
                              {contest.difficulty}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2 text-muted-foreground">
                              <Users className="h-4 w-4" />
                              {contest.participants}
                            </div>
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2">
                              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 border border-primary/30 text-primary text-sm font-medium">
                                {contest.winner.avatar}
                              </div>
                              <span className="text-foreground">{contest.winner.name}</span>
                            </div>
                          </TableCell>
                          <TableCell className="text-right">
                            <Link href={`/contests/${contest.id}`}>
                              <Button variant="ghost" size="sm" className="gap-2 text-muted-foreground hover:text-foreground">
                                Ver Detalles
                                <ExternalLink className="h-4 w-4" />
                              </Button>
                            </Link>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-between px-6 py-4 border-t border-border">
                    <p className="text-sm text-muted-foreground">
                      Mostrando {(currentPage - 1) * itemsPerPage + 1} - {Math.min(currentPage * itemsPerPage, filteredContests.length)} de {filteredContests.length}
                    </p>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                        disabled={currentPage === 1}
                        className="border-border hover:border-primary hover:bg-primary/10"
                      >
                        <ChevronLeft className="h-4 w-4" />
                      </Button>
                      {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                        <Button
                          key={page}
                          variant={currentPage === page ? "default" : "outline"}
                          size="sm"
                          onClick={() => setCurrentPage(page)}
                          className={currentPage === page 
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
                        onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                        disabled={currentPage === totalPages}
                        className="border-border hover:border-primary hover:bg-primary/10"
                      >
                        <ChevronRight className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
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
