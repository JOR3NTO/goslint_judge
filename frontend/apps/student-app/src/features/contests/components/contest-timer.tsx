"use client"

import { useEffect, useState } from "react"
import { Card, CardContent } from "@/shared/ui/primitives/card"
import { Progress } from "@/shared/ui/primitives/progress"
import type { ContestDTO } from "../types/contest.types"
import { msUntil, splitCountdown } from "../utils/format"

interface ContestTimerProps {
  contest: ContestDTO
}

/** Reloj de la maratón calculado con startTime / endTime reales. */
export function ContestTimer({ contest }: ContestTimerProps) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  if (contest.status === "ENDED") {
    return (
      <Card className="border-border bg-card min-w-[280px]">
        <CardContent className="pt-6 text-center">
          <p className="text-sm text-muted-foreground mb-2">Estado</p>
          <p className="text-2xl font-bold text-muted-foreground">Maratón finalizada</p>
        </CardContent>
      </Card>
    )
  }

  const isActive = contest.status === "ACTIVE"
  const target = isActive ? contest.endTime : contest.startTime
  const { hours, minutes, seconds } = splitCountdown(msUntil(target, now))

  const start = new Date(contest.startTime).getTime()
  const end = new Date(contest.endTime).getTime()
  const elapsedPct = end > start ? Math.min(100, Math.max(0, ((now - start) / (end - start)) * 100)) : 0

  return (
    <Card className="border-primary/30 bg-card shadow-sm glow-green-sm min-w-[280px]">
      <CardContent className="pt-6">
        <div className="text-center">
          <p className="text-sm text-muted-foreground mb-2">
            {isActive ? "Tiempo Restante" : "Comienza en"}
          </p>
          <div className="flex items-center justify-center gap-2 text-4xl font-mono font-bold text-primary">
            <span>{String(hours).padStart(2, "0")}</span>
            <span className="animate-pulse">:</span>
            <span>{String(minutes).padStart(2, "0")}</span>
            <span className="animate-pulse">:</span>
            <span>{String(seconds).padStart(2, "0")}</span>
          </div>
          {isActive && (
            <div className="mt-4">
              <Progress value={elapsedPct} className="h-2 bg-secondary" />
              <p className="text-xs text-muted-foreground mt-1">
                {elapsedPct.toFixed(0)}% del tiempo transcurrido
              </p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
