"use client"

import { useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"

export interface ParticipantCounts {
  /** Nº de equipos por id de maratón; `null` si la petición de esa maratón falló. */
  counts: Record<string, number | null>
  /** `true` mientras se piden los conteos de los ids actuales. */
  isLoading: boolean
}

/**
 * Cuenta equipos (participantes) de varias maratones a la vez. Pensado para la página
 * visible de un listado: así la tabla y las tarjetas móviles comparten una sola petición.
 */
export function useParticipantCounts(contestIds: string[]): ParticipantCounts {
  const key = contestIds.join(",")
  const [counts, setCounts] = useState<Record<string, number | null>>({})
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const ids = key ? key.split(",") : []
    if (ids.length === 0) {
      setCounts({})
      setIsLoading(false)
      return
    }

    let cancelled = false
    setIsLoading(true)
    const service = createContestsService()

    Promise.allSettled(ids.map((id) => service.getTeams(id))).then((results) => {
      if (cancelled) return
      const next: Record<string, number | null> = {}
      results.forEach((result, i) => {
        next[ids[i]] = result.status === "fulfilled" ? result.value.length : null
      })
      setCounts(next)
      setIsLoading(false)
    })

    return () => {
      cancelled = true
    }
  }, [key])

  return { counts, isLoading }
}
