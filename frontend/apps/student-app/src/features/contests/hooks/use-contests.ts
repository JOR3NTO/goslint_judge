"use client"

import { useCallback, useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"
import type { ContestDTO, ContestListItem } from "../types/contest.types"
import { contestDurationMs } from "../utils/format"

interface UseContestsReturn {
  contests: ContestListItem[] | null
  isLoading: boolean
  error: string | null
  reload: () => void
}

/** Añade la duración calculada en cliente (endTime - startTime). */
function toListItem(contest: ContestDTO): ContestListItem {
  return {
    ...contest,
    durationMs: contestDurationMs(contest.startTime, contest.endTime),
  }
}

/** Carga el listado público de maratones (`GET /v1/contests`). */
export function useContests(): UseContestsReturn {
  const [contests, setContests] = useState<ContestListItem[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    setIsLoading(true)
    setError(null)

    createContestsService()
      .list()
      .then((list) => {
        if (!cancelled) setContests(list.map(toListItem))
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No se pudieron cargar las maratones.")
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  return { contests, isLoading, error, reload }
}
