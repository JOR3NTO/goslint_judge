"use client"

import Link from "next/link"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { Calendar, Clock, Users, Trophy, ArrowRight } from "lucide-react"

export interface Contest {
  id: string
  title: string
  description: string
  startDate: string
  endDate: string
  duration: string
  participants: number
  difficulty: "Principiante" | "Intermedio" | "Avanzado" | "Experto"
  status: "upcoming" | "active" | "ended"
  problems: number
}

interface ContestCardProps {
  contest: Contest
}

const difficultyColors = {
  Principiante: "bg-green-500/10 text-green-400 border-green-500/30",
  Intermedio: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  Avanzado: "bg-orange-500/10 text-orange-400 border-orange-500/30",
  Experto: "bg-red-500/10 text-red-400 border-red-500/30",
}

const statusColors = {
  upcoming: "bg-blue-500/10 text-blue-400 border-blue-500/30",
  active: "bg-primary/10 text-primary border-primary/30",
  ended: "bg-muted text-muted-foreground border-muted",
}

const statusLabels = {
  upcoming: "Próximamente",
  active: "En Curso",
  ended: "Finalizado",
}

export function ContestCard({ contest }: ContestCardProps) {
  return (
    <Card className="group relative overflow-hidden border-border bg-card hover:border-primary/50 transition-all duration-300 hover:glow-green-sm">
      {/* Glow effect on hover */}
      <div className="absolute inset-0 bg-gradient-to-r from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
      
      <CardHeader className="relative pb-3">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <Badge 
                variant="outline" 
                className={statusColors[contest.status]}
              >
                {statusLabels[contest.status]}
              </Badge>
              <Badge 
                variant="outline" 
                className={difficultyColors[contest.difficulty]}
              >
                {contest.difficulty}
              </Badge>
            </div>
            <h3 className="text-lg font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-1">
              {contest.title}
            </h3>
          </div>
          <Trophy className="h-6 w-6 text-primary/50 group-hover:text-primary transition-colors flex-shrink-0" />
        </div>
        <p className="text-sm text-muted-foreground line-clamp-2">
          {contest.description}
        </p>
      </CardHeader>

      <CardContent className="relative pb-3">
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <Calendar className="h-4 w-4 text-primary/70" />
            <span>{contest.startDate}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Clock className="h-4 w-4 text-primary/70" />
            <span>{contest.duration}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Users className="h-4 w-4 text-primary/70" />
            <span>{contest.participants} participantes</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Trophy className="h-4 w-4 text-primary/70" />
            <span>{contest.problems} problemas</span>
          </div>
        </div>
      </CardContent>

      <CardFooter className="relative pt-3 border-t border-border">
        <Link href={`/contests/${contest.id}`} className="w-full">
          <Button 
            className="w-full gap-2 bg-secondary text-secondary-foreground hover:bg-primary hover:text-primary-foreground transition-all"
            disabled={contest.status === "ended"}
          >
            {contest.status === "ended" ? "Ver Resultados" : contest.status === "active" ? "Entrar al Maratón" : "Ver Detalles"}
            <ArrowRight className="h-4 w-4" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  )
}
