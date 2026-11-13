"use client"

import { useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"
import type { UserSummaryDTO } from "../types/contest.types"

const DEBOUNCE_MS = 250

export interface UseUserSearchReturn {
  users: UserSummaryDTO[]
  loading: boolean
  /** `true` si la última búsqueda falló. */
  failed: boolean
}

/**
 * Busca usuarios por username mientras se escribe (con debounce). Solo consulta cuando
 * `enabled` es `true`, p. ej. al abrir el desplegable. Descarta respuestas de búsquedas viejas.
 */
export function useUserSearch(query: string, enabled: boolean): UseUserSearchReturn {
  const [users, setUsers] = useState<UserSummaryDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    setLoading(true)
    const timer = setTimeout(async () => {
      try {
        const result = await createContestsService().searchUsers(query.trim())
        if (cancelled) return
        setUsers(result)
        setFailed(false)
      } catch {
        if (cancelled) return
        setUsers([])
        setFailed(true)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, DEBOUNCE_MS)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query, enabled])

  return { users, loading, failed }
}
