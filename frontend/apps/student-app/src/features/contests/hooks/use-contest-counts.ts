"use client"

import { useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"

export interface ContestCounts {
  /** Nº de equipos inscritos; `null` si no se pudo obtener. */
  participants: number | null
  /** Nº de problemas de la maratón; `null` si no se pudo obtener. */
  problems: number | null
  isLoading: boolean
}

/**
 * Cuenta participantes (equipos) y problemas de UNA maratón.
 * El backend no los incluye en `ContestDTO`, así que se piden aparte.
 * Las dos peticiones van en paralelo con Promise.allSettled: si una falla,
 * ese dato queda en `null` (la UI muestra "—") sin romper el resto.
 */
export function useContestCounts(contestId: string): ContestCounts {
  const [counts, setCounts] = useState<Omit<ContestCounts, "isLoading">>({
    participants: null,
    problems: null,
  })
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    const service = createContestsService()
    setIsLoading(true)

    Promise.allSettled([service.getTeams(contestId), service.getProblems(contestId)]).then(
      ([teams, problems]) => {
        if (cancelled) return
        setCounts({
          participants: teams.status === "fulfilled" ? teams.value.length : null,
          problems: problems.status === "fulfilled" ? problems.value.length : null,
        })
        setIsLoading(false)
      }
    )

    return () => {
      cancelled = true
    }
  }, [contestId])

  return { ...counts, isLoading }
}
