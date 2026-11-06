"use client"

import { useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"

/**
 * Número de invitaciones PENDING del usuario, para el aviso de la navbar.
 * Best effort: si `enabled` es false o la petición falla, devuelve `null` y la navbar no muestra contador.
 */
export function usePendingInvitationsCount(enabled: boolean): number | null {
  const [count, setCount] = useState<number | null>(null)

  useEffect(() => {
    if (!enabled) {
      setCount(null)
      return
    }
    let cancelled = false
    createContestsService()
      .getMyInvitations()
      .then((list) => {
        if (!cancelled) setCount(list.filter((i) => i.status === "PENDING").length)
      })
      .catch(() => {
        if (!cancelled) setCount(null)
      })
    return () => {
      cancelled = true
    }
  }, [enabled])

  return count
}
