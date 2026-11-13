"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { createClarificationsService } from "../services/clarifications-service"
import type { ClarificationDTO } from "../types/arena.types"

export interface UseClarificationsReturn {
  /** Aclaraciones de la maratón, más reciente primero. */
  items: ClarificationDTO[]
  isLoading: boolean
  /** Error al cargar la lista. */
  error: string | null
  reload: () => void
  /**
   * Envía una pregunta sobre un problema (`contestProblemId` = `ArenaProblem.id`).
   * La añade al principio de la lista. Lanza `ArenaApiError` si el backend la rechaza.
   */
  ask: (contestProblemId: string, question: string) => Promise<ClarificationDTO>
}

/** Aclaraciones de una maratón con alta de preguntas del equipo. */
export function useClarifications(contestId: string): UseClarificationsReturn {
  const [items, setItems] = useState<ClarificationDTO[]>([])
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const service = useMemo(() => createClarificationsService(), [])

  useEffect(() => {
    let cancelled = false

    service
      .getByContest(contestId)
      .then((list) => {
        if (!cancelled) {
          setItems(list)
          setError(null)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "No se pudieron cargar las aclaraciones.")
      })
      .finally(() => {
        if (!cancelled) setLoaded(true)
      })

    return () => {
      cancelled = true
    }
  }, [contestId, attempt, service])

  const reload = useCallback(() => {
    setLoaded(false)
    setAttempt((n) => n + 1)
  }, [])

  const ask = useCallback(
    async (contestProblemId: string, question: string) => {
      const created = await service.ask(contestProblemId, question)
      setItems((prev) => [created, ...prev.filter((item) => item.id !== created.id)])
      return created
    },
    [service]
  )

  return { items, isLoading: !loaded, error, reload, ask }
}
