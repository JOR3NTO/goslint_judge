"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { createContestsService } from "@/features/contests"
import type { TeamDTO } from "@/features/contests"

/** Intervalo del refresco automático mientras la maratón está activa. */
export const SCOREBOARD_REFRESH_MS = 30_000

export interface UseScoreboardReturn {
  /** Equipos ya ordenados por el backend (puntos desc, penalización asc). */
  teams: TeamDTO[]
  /** Primera carga en curso (todavía sin respuesta). */
  isLoading: boolean
  /** Recarga en curso con datos ya visibles. */
  isRefreshing: boolean
  /** Último error; si había datos, se conservan los anteriores. */
  error: string | null
  /** Momento (ms) de la última respuesta correcta. */
  updatedAt: number | null
  refresh: () => void
}

/**
 * Ranking de una maratón (`GET /v1/contests/{id}/scoreboard`).
 * Con `autoRefresh` recarga cada 30 s; el intervalo se limpia al desmontar o al desactivarse.
 * Cada petición descarta la respuesta de la anterior, así que no hay datos viejos sobre nuevos.
 */
export function useScoreboard(contestId: string, autoRefresh: boolean): UseScoreboardReturn {
  const [teams, setTeams] = useState<TeamDTO[]>([])
  const [loaded, setLoaded] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<number | null>(null)
  const [attempt, setAttempt] = useState(0)

  const service = useMemo(() => createContestsService(), [])

  useEffect(() => {
    let cancelled = false
    setIsRefreshing(true)

    service
      .getScoreboard(contestId)
      .then((list) => {
        if (cancelled) return
        setTeams(list)
        setError(null)
        setUpdatedAt(Date.now())
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "No se pudo cargar el ranking.")
      })
      .finally(() => {
        if (cancelled) return
        setIsRefreshing(false)
        setLoaded(true)
      })

    return () => {
      cancelled = true
    }
  }, [contestId, attempt, service])

  useEffect(() => {
    if (!autoRefresh) return
    const timer = setInterval(() => setAttempt((n) => n + 1), SCOREBOARD_REFRESH_MS)
    return () => clearInterval(timer)
  }, [autoRefresh])

  const refresh = useCallback(() => setAttempt((n) => n + 1), [])

  return { teams, isLoading: !loaded, isRefreshing, error, updatedAt, refresh }
}
