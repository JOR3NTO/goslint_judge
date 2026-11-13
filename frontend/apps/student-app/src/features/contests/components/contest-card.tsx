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

/** Acción principal de la tarjeta según el estado: solo la maratón en curso usa el verde sólido. */
const ctaByStatus: Record<ContestStatus, { label: string; variant: "default" | "outline" }> = {
  ACTIVE: { label: "Entrar al maratón", variant: "default" },
  UPCOMING: { label: "Ver detalles", variant: "outline" },
  ENDED: { label: "Ver resultados", variant: "outline" },
}

export function ContestCard({ contest }: ContestCardProps) {
  const { participants, problems, isLoading } = useContestCounts(contest.id)
  const cta = ctaByStatus[contest.status]

  return (
    <Card className="group relative flex h-full flex-col overflow-hidden border-border bg-card shadow-sm transition-all duration-300 hover:border-primary/50 hover:glow-green-sm">
      <CardHeader className="relative pb-3">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1 space-y-2">
            <Badge variant="outline" className={statusColors[contest.status]}>
              {statusLabels[contest.status]}
            </Badge>
            <h3
              title={contest.title}
              className="min-h-[3.25rem] line-clamp-2 text-lg font-semibold leading-snug text-foreground transition-colors group-hover:text-primary"
            >
              {contest.title}
            </h3>
          </div>
          <Trophy aria-hidden="true" className="h-6 w-6 flex-shrink-0 text-primary/50 transition-colors group-hover:text-primary" />
        </div>
        <p className="min-h-[2.5rem] line-clamp-2 text-sm text-muted-foreground">
          {contest.description}
        </p>
      </CardHeader>

      <CardContent className="relative pb-3">
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="flex items-center gap-2 text-muted-foreground">
            <Calendar aria-hidden="true" className="h-4 w-4 flex-shrink-0 text-primary/70" />
            <span>{formatDate(contest.startTime)}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Clock aria-hidden="true" className="h-4 w-4 flex-shrink-0 text-primary/70" />
            <span>{formatDuration(contest.durationMs)}</span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Users aria-hidden="true" className="h-4 w-4 flex-shrink-0 text-primary/70" />
            <span>
              {isLoading ? <Skeleton className="inline-block h-4 w-20 align-middle" /> : `${participants ?? "—"} participantes`}
            </span>
          </div>
          <div className="flex items-center gap-2 text-muted-foreground">
            <Trophy aria-hidden="true" className="h-4 w-4 flex-shrink-0 text-primary/70" />
            <span>
              {isLoading ? <Skeleton className="inline-block h-4 w-16 align-middle" /> : `${problems ?? "—"} problemas`}
            </span>
          </div>
        </div>
      </CardContent>

      <CardFooter className="relative mt-auto border-t border-border pt-3">
        <Button asChild variant={cta.variant} className="w-full gap-2">
          <Link href={`/contests/${contest.id}`}>
            {cta.label}
            <ArrowRight aria-hidden="true" className="h-4 w-4" />
            <span className="sr-only">({contest.title})</span>
          </Link>
        </Button>
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
