"use client"

import { Badge } from "@/shared/ui/primitives/badge"
import { Progress } from "@/shared/ui/primitives/progress"
import { cn } from "@/shared/utils/cn"
import { ContestTimer, type ContestDTO, type ContestStatus } from "@/features/contests"

const statusColors: Record<ContestStatus, string> = {
  UPCOMING: "bg-blue-500/10 text-blue-400 border-blue-500/30",
  ACTIVE: "bg-primary/10 text-primary border-primary/30",
  ENDED: "bg-muted text-muted-foreground border-muted",
}

const statusLabels: Record<ContestStatus, string> = {
  UPCOMING: "Próximamente",
  ACTIVE: "En Curso",
  ENDED: "Finalizado",
}

interface ArenaHeaderProps {
  contest: ContestDTO
  accepted: number
  total: number
}

/** Cabecera: nombre y estado del maratón, cuenta regresiva y progreso del equipo. */
export function ArenaHeader({ contest, accepted, total }: ArenaHeaderProps) {
  const percent = total > 0 ? Math.round((accepted / total) * 100) : 0

  return (
    <section className="border-b border-border bg-card/40">
      <div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
        <div className="min-w-0 flex-1 space-y-3">
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant="outline" className={cn(statusColors[contest.status])}>
              {statusLabels[contest.status]}
            </Badge>
          </div>
          <h1 className="text-3xl font-bold text-foreground sm:text-4xl">{contest.title}</h1>

          <div className="max-w-xl space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-foreground">
                Progreso: {accepted} / {total} problemas
              </span>
              <span className="font-mono text-muted-foreground">{percent}%</span>
            </div>
            <Progress value={percent} className="h-2 bg-secondary" aria-label="Progreso del equipo" />
          </div>
        </div>

        <div className="w-full lg:w-auto">
          <ContestTimer contest={contest} />
        </div>
      </div>
    </section>
  )
}
