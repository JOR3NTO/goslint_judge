"use client"

import { useState, useEffect } from "react"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "@/shared/ui/primitives/tabs"
import { env } from "@/core/config/env"
import { getPublicContests } from "../services/public-contests-service"
import type { PublicContest, ContestStatus } from "../types/public-contest.types"
import {
  Trophy,
  Lock,
  Users,
  Clock,
  Calendar,
  Zap,
  ArrowRight,
  BookOpen,
} from "lucide-react"

// ─── ContestCard pública ──────────────────────────────────────────────────────

function PublicContestCard({ contest }: { contest: PublicContest }) {
  const statusConfig: Record<
    ContestStatus,
    { label: string; className: string; dotClass: string }
  > = {
    active: {
      label: "En curso",
      className: "bg-primary/10 text-primary border-primary/30",
      dotClass: "bg-primary animate-pulse",
    },
    upcoming: {
      label: "Próxima",
      className: "bg-blue-500/10 text-blue-400 border-blue-500/30",
      dotClass: "bg-blue-400",
    },
    past: {
      label: "Finalizada",
      className: "bg-muted text-muted-foreground border-border",
      dotClass: "bg-muted-foreground",
    },
  }

  const difficultyColor: Record<string, string> = {
    Principiante: "bg-green-500/10 text-green-400 border-green-500/30",
    Intermedio: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
    Avanzado: "bg-red-500/10 text-red-400 border-red-500/30",
  }

  const cfg = statusConfig[contest.status]

  return (
    <div className="group relative overflow-hidden rounded-xl border border-border bg-card hover:border-primary/40 transition-all duration-300 flex flex-col">
      {/* Top gradient on hover */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />

      <div className="relative p-6 flex flex-col gap-4 flex-1">
        {/* Header row */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 border border-primary/30">
            <Trophy className="h-5 w-5 text-primary" />
          </div>
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-xs font-medium shrink-0"
            style={{}}
          >
            <span className={`h-1.5 w-1.5 rounded-full ${cfg.dotClass}`} />
            <span className={cfg.className.split(" ").filter(c => c.startsWith("text-")).join(" ")}>
              {cfg.label}
            </span>
          </div>
        </div>

        {/* Title & description */}
        <div className="space-y-1.5">
          <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2 leading-snug">
            {contest.title}
          </h3>
          <p className="text-sm text-muted-foreground line-clamp-2">
            {contest.description}
          </p>
        </div>

        {/* Meta info */}
        <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5 shrink-0" />
            <span>{contest.startDate}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Clock className="h-3.5 w-3.5 shrink-0" />
            <span>{contest.duration}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Users className="h-3.5 w-3.5 shrink-0" />
            <span>
              {contest.status === "upcoming"
                ? "Abierto"
                : `${contest.participantCount.toLocaleString()} participantes`}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <BookOpen className="h-3.5 w-3.5 shrink-0" />
            <span>{contest.problemCount} problemas</span>
          </div>
        </div>

        {/* Tags */}
        <div className="flex gap-2 flex-wrap">
          <span
            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md border text-xs font-medium ${difficultyColor[contest.difficulty]}`}
          >
            <Zap className="h-3 w-3" />
            {contest.difficulty}
          </span>
        </div>

        {/* CTA */}
        <div className="mt-auto pt-2">
          <a href={`${env.APP_URL}/login`} className="block">
            <Button
              variant="outline"
              className="w-full gap-2 border-border hover:border-primary hover:bg-primary/10 group/btn"
            >
              <Lock className="h-3.5 w-3.5 text-muted-foreground group-hover/btn:text-primary transition-colors" />
              {contest.status === "past" ? "Ver resultados" : "Participar"}
              <ArrowRight className="h-3.5 w-3.5 ml-auto" />
            </Button>
          </a>
        </div>
      </div>
    </div>
  )
}

// ─── ContestsPreview ─────────────────────────────────────────────────────────

const TAB_LABELS: Record<ContestStatus, string> = {
  active: "● Activas",
  upcoming: "Próximas",
  past: "Pasadas",
}

export function ContestsPreview() {
  const [activeContests, setActiveContests] = useState<PublicContest[]>([])
  const [upcomingContests, setUpcomingContests] = useState<PublicContest[]>([])
  const [pastContests, setPastContests] = useState<PublicContest[]>([])

  useEffect(() => {
    getPublicContests("active").then(setActiveContests)
    getPublicContests("upcoming").then(setUpcomingContests)
    getPublicContests("past").then(setPastContests)
  }, [])

  const contestsByTab: Record<ContestStatus, PublicContest[]> = {
    active: activeContests,
    upcoming: upcomingContests,
    past: pastContests,
  }

  return (
    <section id="maratones" className="py-24 relative">
      <div className="absolute inset-0 from-primary/5 via-transparent to-transparent"
        style={{ backgroundImage: "radial-gradient(ellipse at center, var(--tw-gradient-stops))" }}
      />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center mb-12">
          <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
            Maratones <span className="text-primary">Disponibles</span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Explora las competencias disponibles. Para participar, crea tu cuenta o inicia sesión.
          </p>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="active" className="w-full">
          <div className="flex justify-center mb-8">
            <TabsList className="bg-secondary/50 border border-border p-1">
              {(["active", "upcoming", "past"] as ContestStatus[]).map((tab) => (
                <TabsTrigger
                  key={tab}
                  value={tab}
                  className="gap-2 data-[state=active]:bg-primary/10 data-[state=active]:text-primary data-[state=active]:border-primary/30 px-4"
                >
                  {TAB_LABELS[tab]}
                  <span className="ml-1 text-xs opacity-60">
                    ({contestsByTab[tab].length})
                  </span>
                </TabsTrigger>
              ))}
            </TabsList>
          </div>

          {(["active", "upcoming", "past"] as ContestStatus[]).map((tab) => (
            <TabsContent key={tab} value={tab}>
              {contestsByTab[tab].length === 0 ? (
                <div className="text-center py-16 text-muted-foreground">
                  <Trophy className="h-12 w-12 mx-auto mb-4 opacity-20" />
                  <p className="text-lg">No hay maratones {TAB_LABELS[tab].toLowerCase()} por ahora.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {contestsByTab[tab].map((contest) => (
                    <PublicContestCard key={contest.id} contest={contest} />
                  ))}
                </div>
              )}
            </TabsContent>
          ))}
        </Tabs>

        {/* Bottom CTA */}
        <div className="mt-16 text-center space-y-4">
          <p className="text-muted-foreground">
            ¿Quieres participar en estas maratones?
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <a href={`${env.APP_URL}/register`}>
              <Button
                size="lg"
                className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm px-8"
              >
                Crear Cuenta Gratis
                <ArrowRight className="h-5 w-5" />
              </Button>
            </a>
            <a href={`${env.APP_URL}/login`}>
              <Button
                size="lg"
                variant="outline"
                className="gap-2 border-border hover:border-primary hover:bg-primary/10 px-8"
              >
                Ya tengo cuenta — Iniciar Sesión
              </Button>
            </a>
          </div>
        </div>
      </div>
    </section>
  )
}
