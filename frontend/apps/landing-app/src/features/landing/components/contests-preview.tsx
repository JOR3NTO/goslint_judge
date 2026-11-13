"use client"

import { useEffect, useState } from "react"
import { Button } from "@/shared/ui/primitives/button"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/primitives/tabs"
import { env } from "@/core/config/env"
import { getPublicContests } from "../services/public-contests-service"
import type { ContestStatus, PublicContest } from "../types/public-contest.types"
import { ArrowRight, Calendar, CircleAlert, Clock, RotateCw, Trophy } from "lucide-react"

const TABS: { value: ContestStatus; label: string }[] = [
  { value: "active", label: "Activas" },
  { value: "upcoming", label: "Próximas" },
  { value: "past", label: "Pasadas" },
]

const STATUS_BADGE: Record<ContestStatus, { label: string; className: string; dotClass: string }> = {
  active: {
    label: "En curso",
    className: "border-primary/30 bg-primary/10 text-primary",
    dotClass: "bg-primary",
  },
  upcoming: {
    label: "Próxima",
    className: "border-info/30 bg-info/10 text-info",
    dotClass: "bg-info",
  },
  past: {
    label: "Finalizada",
    className: "border-border bg-muted text-muted-foreground",
    dotClass: "bg-muted-foreground",
  },
}

/** Zona horaria de Colombia, para mostrar las fechas en hora local del semillero. */
const dateFormatter = new Intl.DateTimeFormat("es-CO", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "America/Bogota",
})

function formatDate(date: Date): string {
  return dateFormatter.format(date)
}

/** Duración en formato corto: "45 min", "3 h", "2 h 30 min", "1 día 4 h". */
function formatDuration(ms: number): string {
  const totalMinutes = Math.max(0, Math.round(ms / 60000))
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60

  if (days > 0) {
    const dayLabel = days === 1 ? "1 día" : `${days} días`
    return hours > 0 ? `${dayLabel} ${hours} h` : dayLabel
  }
  if (hours > 0) {
    return minutes > 0 ? `${hours} h ${minutes} min` : `${hours} h`
  }
  return `${minutes} min`
}

function ContestCard({ contest }: { contest: PublicContest }) {
  const badge = STATUS_BADGE[contest.status]
  const duration = formatDuration(contest.endTime.getTime() - contest.startTime.getTime())

  return (
    <article className="group flex flex-col gap-4 rounded-xl border border-border bg-card p-6 transition-colors hover:border-primary/40">
      <div className="flex items-start justify-between gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-primary/30 bg-primary/10">
          <Trophy className="h-5 w-5 text-primary" aria-hidden="true" />
        </div>
        <span
          className={`inline-flex shrink-0 items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium ${badge.className}`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${badge.dotClass}`} aria-hidden="true" />
          {badge.label}
        </span>
      </div>

      <div className="space-y-2">
        <h3 className="line-clamp-2 font-semibold leading-snug text-foreground transition-colors group-hover:text-primary">
          {contest.title}
        </h3>
        {contest.description && (
          <p className="line-clamp-3 text-sm text-muted-foreground">{contest.description}</p>
        )}
      </div>

      <ul className="space-y-1.5 text-xs text-muted-foreground">
        <li className="flex items-center gap-1.5">
          <Calendar className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span>Inicio: {formatDate(contest.startTime)}</span>
        </li>
        <li className="flex items-center gap-1.5">
          <Calendar className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span>Fin: {formatDate(contest.endTime)}</span>
        </li>
        <li className="flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          <span>Duración: {duration}</span>
        </li>
      </ul>

      <div className="mt-auto pt-2">
        {contest.status === "past" ? (
          <p className="text-center text-sm text-muted-foreground">Maratón finalizada</p>
        ) : (
          <Button asChild className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
            <a href={`${env.APP_URL}/login`}>
              Participar
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </a>
          </Button>
        )}
      </div>
    </article>
  )
}

function ContestCardSkeleton() {
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border bg-card p-6" aria-hidden="true">
      <div className="flex items-start justify-between">
        <div className="h-10 w-10 animate-pulse rounded-lg bg-muted" />
        <div className="h-6 w-20 animate-pulse rounded-full bg-muted" />
      </div>
      <div className="space-y-2">
        <div className="h-5 w-3/4 animate-pulse rounded bg-muted" />
        <div className="h-4 w-full animate-pulse rounded bg-muted" />
      </div>
      <div className="space-y-1.5">
        <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
        <div className="h-3 w-2/3 animate-pulse rounded bg-muted" />
        <div className="h-3 w-1/3 animate-pulse rounded bg-muted" />
      </div>
      <div className="mt-auto h-9 w-full animate-pulse rounded-md bg-muted" />
    </div>
  )
}

type LoadState =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; contests: PublicContest[] }

export function ContestsPreview() {
  const [state, setState] = useState<LoadState>({ status: "loading" })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getPublicContests(controller.signal)
      .then((contests) => {
        if (!controller.signal.aborted) setState({ status: "ready", contests })
      })
      .catch(() => {
        if (!controller.signal.aborted) setState({ status: "error" })
      })
    return () => controller.abort()
  }, [attempt])

  const retry = () => {
    setState({ status: "loading" })
    setAttempt((n) => n + 1)
  }

  const contestsFor = (status: ContestStatus): PublicContest[] =>
    state.status === "ready" ? state.contests.filter((c) => c.status === status) : []

  const countFor = (status: ContestStatus): number | null =>
    state.status === "ready" ? contestsFor(status).length : null

  return (
    <section id="maratones" className="relative scroll-mt-24 py-24">
      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto mb-12 max-w-2xl text-center">
          <h2 className="mb-4 text-3xl font-bold text-foreground sm:text-4xl">
            Maratones <span className="text-primary">publicadas</span>
          </h2>
          <p className="text-lg text-muted-foreground">
            Consulta las maratones disponibles. Para participar necesitas una cuenta.
          </p>
        </div>

        <Tabs defaultValue="active" className="w-full">
          <div className="mb-8 flex justify-center">
            <TabsList className="border border-border bg-secondary/50 p-1">
              {TABS.map((tab) => {
                const count = countFor(tab.value)
                return (
                  <TabsTrigger
                    key={tab.value}
                    value={tab.value}
                    className="gap-2 px-4 data-[state=active]:border-primary/30 data-[state=active]:bg-primary/10 data-[state=active]:text-primary"
                  >
                    {tab.label}
                    {count !== null && <span className="text-xs opacity-70">({count})</span>}
                  </TabsTrigger>
                )
              })}
            </TabsList>
          </div>

          {TABS.map((tab) => (
            <TabsContent key={tab.value} value={tab.value}>
              {state.status === "loading" && (
                <div
                  className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3"
                  role="status"
                  aria-label="Cargando maratones"
                >
                  <ContestCardSkeleton />
                  <ContestCardSkeleton />
                  <ContestCardSkeleton />
                </div>
              )}

              {state.status === "error" && (
                <div
                  role="alert"
                  className="mx-auto max-w-xl rounded-xl border border-border bg-card p-8 text-center"
                >
                  <CircleAlert className="mx-auto h-10 w-10 text-destructive" aria-hidden="true" />
                  <p className="mt-4 font-semibold text-foreground">No pudimos cargar las maratones</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Revisa tu conexión e inténtalo de nuevo.
                  </p>
                  <Button variant="outline" onClick={retry} className="mt-6 gap-2">
                    <RotateCw className="h-4 w-4" aria-hidden="true" />
                    Reintentar
                  </Button>
                </div>
              )}

              {state.status === "ready" &&
                (contestsFor(tab.value).length === 0 ? (
                  <div className="py-16 text-center text-muted-foreground">
                    <Trophy className="mx-auto mb-4 h-12 w-12 opacity-20" aria-hidden="true" />
                    <p className="text-lg">Aún no hay maratones publicadas</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {contestsFor(tab.value).map((contest) => (
                      <ContestCard key={contest.id} contest={contest} />
                    ))}
                  </div>
                ))}
            </TabsContent>
          ))}
        </Tabs>
      </div>
    </section>
  )
}
