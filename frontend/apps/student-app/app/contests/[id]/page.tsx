"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { 
  ArrowLeft, 
  Trophy, 
  Clock, 
  Users, 
  Calendar,
  Play,
  FileCode2,
  Medal,
  CheckCircle2,
  XCircle,
  Timer
} from "lucide-react"

interface Problem {
  id: string
  letter: string
  title: string
  difficulty: "Fácil" | "Medio" | "Difícil" | "Muy Difícil"
  solved: number
  attempts: number
}

interface Participant {
  rank: number
  username: string
  avatar: string
  solved: number
  penalty: number
  lastSubmission: string
}

const contestData = {
  id: "2",
  title: "Maratón Nacional de Algoritmos",
  description: "La competencia más grande del año. Premios especiales para los top 10. Incluye problemas de grafos, programación dinámica, estructuras de datos y matemáticas.",
  startDate: "20 Ene 2025, 10:00 AM",
  endDate: "20 Ene 2025, 3:00 PM",
  duration: "5 horas",
  participants: 1205,
  difficulty: "Avanzado",
  status: "active",
  rules: [
    "Cada problema resuelto suma puntos según su dificultad",
    "El tiempo de penalización es de 20 minutos por intento fallido",
    "Puedes usar cualquier lenguaje de programación soportado",
    "No se permite el uso de herramientas de IA para resolver problemas",
    "Los resultados finales se publicarán 30 minutos después de finalizar"
  ]
}

const problems: Problem[] = [
  { id: "A", letter: "A", title: "Suma de Números", difficulty: "Fácil", solved: 1102, attempts: 1350 },
  { id: "B", letter: "B", title: "Ordenamiento de Strings", difficulty: "Fácil", solved: 987, attempts: 1420 },
  { id: "C", letter: "C", title: "Árbol de Segmentos", difficulty: "Medio", solved: 654, attempts: 1890 },
  { id: "D", letter: "D", title: "Flujo Máximo en Red", difficulty: "Medio", solved: 432, attempts: 2100 },
  { id: "E", letter: "E", title: "Programación Dinámica en Árboles", difficulty: "Difícil", solved: 234, attempts: 1560 },
  { id: "F", letter: "F", title: "Teoría de Juegos", difficulty: "Difícil", solved: 145, attempts: 980 },
  { id: "G", letter: "G", title: "Geometría Computacional", difficulty: "Muy Difícil", solved: 67, attempts: 540 },
  { id: "H", letter: "H", title: "FFT y Polinomios", difficulty: "Muy Difícil", solved: 23, attempts: 320 },
  { id: "I", letter: "I", title: "Suffix Array con LCP", difficulty: "Muy Difícil", solved: 12, attempts: 210 },
  { id: "J", letter: "J", title: "Persistente Data Structures", difficulty: "Muy Difícil", solved: 5, attempts: 145 },
]

const leaderboard: Participant[] = [
  { rank: 1, username: "AlgoNinja", avatar: "A", solved: 9, penalty: 324, lastSubmission: "hace 15 min" },
  { rank: 2, username: "CodeMaster42", avatar: "C", solved: 9, penalty: 456, lastSubmission: "hace 8 min" },
  { rank: 3, username: "ByteRunner", avatar: "B", solved: 8, penalty: 289, lastSubmission: "hace 22 min" },
  { rank: 4, username: "GraphTheorist", avatar: "G", solved: 8, penalty: 367, lastSubmission: "hace 5 min" },
  { rank: 5, username: "DPMaster", avatar: "D", solved: 7, penalty: 234, lastSubmission: "hace 30 min" },
  { rank: 6, username: "FastSolver", avatar: "F", solved: 7, penalty: 298, lastSubmission: "hace 12 min" },
  { rank: 7, username: "AlgoKing", avatar: "A", solved: 6, penalty: 187, lastSubmission: "hace 45 min" },
  { rank: 8, username: "NewCoder", avatar: "N", solved: 6, penalty: 234, lastSubmission: "hace 18 min" },
]

const difficultyColors = {
  "Fácil": "bg-green-500/10 text-green-400 border-green-500/30",
  "Medio": "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  "Difícil": "bg-orange-500/10 text-orange-400 border-orange-500/30",
  "Muy Difícil": "bg-red-500/10 text-red-400 border-red-500/30",
}

export default function ContestDetailPage() {
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 34, seconds: 56 })
  const [progress, setProgress] = useState(50)

  // Simulate countdown
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(prev => {
        let { hours, minutes, seconds } = prev
        seconds--
        if (seconds < 0) {
          seconds = 59
          minutes--
        }
        if (minutes < 0) {
          minutes = 59
          hours--
        }
        if (hours < 0) {
          hours = 0
          minutes = 0
          seconds = 0
        }
        return { hours, minutes, seconds }
      })
      setProgress(prev => Math.min(prev + 0.01, 100))
    }, 1000)

    return () => clearInterval(timer)
  }, [])

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

            <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
              <div className="space-y-4">
                <div className="flex items-center gap-3 flex-wrap">
                  <Badge className="bg-primary/10 text-primary border-primary/30 hover:bg-primary/20">
                    En Curso
                  </Badge>
                  <Badge variant="outline" className="bg-orange-500/10 text-orange-400 border-orange-500/30">
                    {contestData.difficulty}
                  </Badge>
                </div>
                <h1 className="text-3xl sm:text-4xl font-bold text-foreground">
                  {contestData.title}
                </h1>
                <p className="text-muted-foreground max-w-2xl">
                  {contestData.description}
                </p>
                <div className="flex flex-wrap items-center gap-6 text-sm text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <Calendar className="h-4 w-4 text-primary" />
                    {contestData.startDate}
                  </div>
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-primary" />
                    {contestData.duration}
                  </div>
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-primary" />
                    {contestData.participants} participantes
                  </div>
                </div>
              </div>

              {/* Timer Card */}
              <Card className="border-primary/30 bg-card glow-green-sm min-w-[280px]">
                <CardContent className="pt-6">
                  <div className="text-center">
                    <p className="text-sm text-muted-foreground mb-2">Tiempo Restante</p>
                    <div className="flex items-center justify-center gap-2 text-4xl font-mono font-bold text-primary">
                      <span>{String(timeLeft.hours).padStart(2, '0')}</span>
                      <span className="animate-pulse">:</span>
                      <span>{String(timeLeft.minutes).padStart(2, '0')}</span>
                      <span className="animate-pulse">:</span>
                      <span>{String(timeLeft.seconds).padStart(2, '0')}</span>
                    </div>
                    <div className="mt-4">
                      <Progress value={progress} className="h-2 bg-secondary" />
                      <p className="text-xs text-muted-foreground mt-1">
                        {progress.toFixed(0)}% del tiempo transcurrido
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </section>

        {/* Content */}
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
                <TabsTrigger value="rules" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
                  <Timer className="h-4 w-4 mr-2" />
                  Reglas
                </TabsTrigger>
              </TabsList>

              {/* Problems Tab */}
              <TabsContent value="problems">
                <Card className="border-border bg-card">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <FileCode2 className="h-5 w-5 text-primary" />
                      Problemas ({problems.length})
                    </CardTitle>
                    <CardDescription>
                      Selecciona un problema para comenzar a resolverlo
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow className="border-border hover:bg-transparent">
                            <TableHead className="text-muted-foreground w-16">#</TableHead>
                            <TableHead className="text-muted-foreground">Problema</TableHead>
                            <TableHead className="text-muted-foreground">Dificultad</TableHead>
                            <TableHead className="text-muted-foreground text-center">Resueltos</TableHead>
                            <TableHead className="text-muted-foreground text-center">Ratio</TableHead>
                            <TableHead className="text-muted-foreground text-right">Acción</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {problems.map((problem) => {
                            const ratio = ((problem.solved / problem.attempts) * 100).toFixed(0)
                            return (
                              <TableRow key={problem.id} className="border-border hover:bg-secondary/50">
                                <TableCell>
                                  <span className="flex h-8 w-8 items-center justify-center rounded bg-primary/10 text-primary font-mono font-bold">
                                    {problem.letter}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <p className="font-medium text-foreground">{problem.title}</p>
                                </TableCell>
                                <TableCell>
                                  <Badge variant="outline" className={difficultyColors[problem.difficulty]}>
                                    {problem.difficulty}
                                  </Badge>
                                </TableCell>
                                <TableCell className="text-center">
                                  <div className="flex items-center justify-center gap-1 text-muted-foreground">
                                    <CheckCircle2 className="h-4 w-4 text-primary" />
                                    {problem.solved}
                                  </div>
                                </TableCell>
                                <TableCell className="text-center">
                                  <span className={`font-mono ${Number(ratio) > 50 ? 'text-primary' : 'text-orange-400'}`}>
                                    {ratio}%
                                  </span>
                                </TableCell>
                                <TableCell className="text-right">
                                  <Button size="sm" className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                                    <Play className="h-4 w-4" />
                                    Resolver
                                  </Button>
                                </TableCell>
                              </TableRow>
                            )
                          })}
                        </TableBody>
                      </Table>
                    </div>
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
                      Actualizado en tiempo real
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow className="border-border hover:bg-transparent">
                            <TableHead className="text-muted-foreground w-16">Rank</TableHead>
                            <TableHead className="text-muted-foreground">Usuario</TableHead>
                            <TableHead className="text-muted-foreground text-center">Resueltos</TableHead>
                            <TableHead className="text-muted-foreground text-center">Penalización</TableHead>
                            <TableHead className="text-muted-foreground text-right">Última Entrega</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {leaderboard.map((participant) => (
                            <TableRow key={participant.rank} className="border-border hover:bg-secondary/50">
                              <TableCell>
                                {participant.rank <= 3 ? (
                                  <div className={`flex h-8 w-8 items-center justify-center rounded-full ${
                                    participant.rank === 1 ? 'bg-yellow-500/20 text-yellow-400' :
                                    participant.rank === 2 ? 'bg-gray-400/20 text-gray-400' :
                                    'bg-orange-500/20 text-orange-400'
                                  }`}>
                                    <Medal className="h-4 w-4" />
                                  </div>
                                ) : (
                                  <span className="text-muted-foreground font-mono">{participant.rank}</span>
                                )}
                              </TableCell>
                              <TableCell>
                                <div className="flex items-center gap-3">
                                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 border border-primary/30 text-primary font-medium">
                                    {participant.avatar}
                                  </div>
                                  <span className="font-medium text-foreground">{participant.username}</span>
                                </div>
                              </TableCell>
                              <TableCell className="text-center">
                                <span className="font-bold text-primary">{participant.solved}</span>
                                <span className="text-muted-foreground">/{problems.length}</span>
                              </TableCell>
                              <TableCell className="text-center font-mono text-muted-foreground">
                                {participant.penalty} min
                              </TableCell>
                              <TableCell className="text-right text-muted-foreground text-sm">
                                {participant.lastSubmission}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>

              {/* Rules Tab */}
              <TabsContent value="rules">
                <Card className="border-border bg-card">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Timer className="h-5 w-5 text-primary" />
                      Reglas del Maratón
                    </CardTitle>
                    <CardDescription>
                      Lee atentamente las reglas antes de comenzar
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    <ul className="space-y-4">
                      {contestData.rules.map((rule, index) => (
                        <li key={index} className="flex items-start gap-3">
                          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-medium flex-shrink-0">
                            {index + 1}
                          </div>
                          <span className="text-muted-foreground">{rule}</span>
                        </li>
                      ))}
                    </ul>

                    <div className="mt-8 p-4 rounded-lg bg-primary/10 border border-primary/30">
                      <h4 className="font-medium text-foreground mb-2 flex items-center gap-2">
                        <CheckCircle2 className="h-5 w-5 text-primary" />
                        Lenguajes Soportados
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {["C++", "Python", "Java", "JavaScript", "Go", "Rust", "C#", "Ruby", "Kotlin"].map((lang) => (
                          <Badge key={lang} variant="outline" className="border-border">
                            {lang}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
