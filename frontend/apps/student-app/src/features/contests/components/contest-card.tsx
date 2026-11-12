"use client"

import Link from "next/link"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/shared/ui/primitives/card"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { Calendar, Clock, Users, Trophy, ArrowRight } from "lucide-react"
import { useContestCounts } from "../hooks/use-contest-counts"
import type { ContestListItem, ContestStatus } from "../types/contest.types"
import { formatDate, formatDuration } from "../utils/format"

interface ContestCardProps {
  contest: ContestListItem
}

const statusColors: Record<ContestStatus, string> = {
  UPCOMING: "bg-info/10 text-info border-info/30",
  ACTIVE: "bg-primary/10 text-primary border-primary/30",
  ENDED: "bg-muted text-muted-foreground border-muted",
}

const statusLabels: Record<ContestStatus, string> = {
  UPCOMING: "Próximamente",
  ACTIVE: "En Curso",
  ENDED: "Finalizado",
}

export function ContestCard({ contest }: ContestCardProps) {
  const { participants, problems, isLoading } = useContestCounts(contest.id)
  const isEnded = contest.status === "ENDED"

  return (
    <Card className="group relative overflow-hidden border-border bg-card shadow-sm hover:border-primary/50 transition-all duration-300 hover:glow-green-sm">
      <CardHeader className="relative pb-3">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1 flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <Badge variant="outline" className={statusColors[contest.status]}>
                {statusLabels[contest.status]}
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
            <span>{formatDate(contest.startTime)}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Clock className="h-4 w-4 text-primary/70" />
            <span>{formatDuration(contest.durationMs)}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Users className="h-4 w-4 text-primary/70" />
            <span>
              {isLoading ? <Skeleton className="inline-block h-4 w-20 align-middle" /> : `${participants ?? "—"} participantes`}
            </span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Trophy className="h-4 w-4 text-primary/70" />
            <span>
              {isLoading ? <Skeleton className="inline-block h-4 w-16 align-middle" /> : `${problems ?? "—"} problemas`}
            </span>
          </div>
        </div>
      </CardContent>

      <CardFooter className="relative pt-3 border-t border-border">
        <Link href={`/contests/${contest.id}`} className="w-full">
          <Button
            className="w-full gap-2 bg-secondary text-secondary-foreground hover:bg-primary hover:text-primary-foreground transition-all"
            disabled={isEnded}
          >
            {isEnded ? "Ver Resultados" : contest.status === "ACTIVE" ? "Entrar al Maratón" : "Ver Detalles"}
            <ArrowRight className="h-4 w-4" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  )
}

/** Placeholder con la misma forma que `ContestCard` mientras carga el listado. */
export function ContestCardSkeleton() {
  return (
    <Card className="border-border bg-card">
      <CardHeader className="pb-3 space-y-3">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-6 w-3/4" />
        <Skeleton className="h-4 w-full" />
      </CardHeader>
      <CardContent className="pb-3">
        <div className="grid grid-cols-2 gap-3">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
        </div>
      </CardContent>
      <CardFooter className="pt-3 border-t border-border">
        <Skeleton className="h-9 w-full" />
      </CardFooter>
    </Card>
  )
}
