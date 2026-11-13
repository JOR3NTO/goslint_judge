"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { env } from "@/core/config/env"
import {
  createContestsService,
  subscribeScoreboardStream,
  applyTeamPositionUpdate,
} from "@/features/contests"
import type { TeamDTO } from "@/features/contests"

/** Intervalo del refresco automático en modo mock, que no tiene stream. */
export const SCOREBOARD_REFRESH_MS = 30_000

export interface UseScoreboardReturn {
  /** Equipos ya ordenados por el backend (puntos desc, penalización asc, último problema resuelto antes). */
  teams: TeamDTO[]
  /** Primera carga en curso (todavía sin respuesta). */
  isLoading: boolean
  /** Recarga en curso con datos ya visibles. */
  isRefreshing: boolean
  /** Último error; si había datos, se conservan los anteriores. */
  error: string | null
  /** Momento (ms) de la última respuesta o cambio recibido. */
  updatedAt: number | null
  /** La tabla pública está congelada: no muestra los últimos minutos hasta que el organizador la libere. */
  frozen: boolean
  /** Conectado al stream: los cambios llegan solos. */
  live: boolean
  refresh: () => void
}

/**
 * Ranking de una maratón. Con el backend real escucha el stream SSE
 * (`GET /v1/contests/{id}/scoreboard/stream`): primero `frozen` y la tabla completa (`scoreboard`),
 * después un evento `team` por cada equipo que cambia de posición. `EventSource` reconecta solo
 * y al reconectar vuelve a recibir la tabla completa.
 * En modo mock no hay stream: con `autoRefresh` consulta `GET /v1/contests/{id}/scoreboard` cada 30 s.
 */
export function useScoreboard(contestId: string, autoRefresh: boolean): UseScoreboardReturn {
  const [teams, setTeams] = useState<TeamDTO[]>([])
  const [loaded, setLoaded] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [updatedAt, setUpdatedAt] = useState<number | null>(null)
  const [frozen, setFrozen] = useState(false)
  const [live, setLive] = useState(false)
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
    if (env.MOCK_ENABLED) return
    const received = () => {
      setUpdatedAt(Date.now())
      setError(null)
      setLoaded(true)
    }
    const close = subscribeScoreboardStream(contestId, {
      onOpen: () => setLive(true),
      onError: () => setLive(false),
      onFrozen: setFrozen,
      onScoreboard: (nextTeams) => {
        setTeams(nextTeams)
        received()
      },
      onTeamUpdate: (position, team) => {
        setTeams((current) => applyTeamPositionUpdate(current, position, team))
        received()
      },
    })
    return () => {
      close()
      setLive(false)
    }
  }, [contestId])

  useEffect(() => {
    if (!autoRefresh || !env.MOCK_ENABLED) return
    const timer = setInterval(() => setAttempt((n) => n + 1), SCOREBOARD_REFRESH_MS)
    return () => clearInterval(timer)
  }, [autoRefresh])

  const refresh = useCallback(() => setAttempt((n) => n + 1), [])

  return { teams, isLoading: !loaded, isRefreshing, error, updatedAt, frozen, live, refresh }
}
