"use client"

import { useEffect, useState } from "react"

import { hasRecoverableSession } from "@/core/auth/session"

/** Clave de localStorage del access token (la define core/auth/session). */
export { AUTH_TOKEN_KEY } from "@/core/auth/session"

/**
 * Indica si hay una sesión utilizable en el navegador: access token vigente, o
 * caducado pero con refresh token para renovarlo.
 *
 * `null` mientras no se ha leído localStorage (render de servidor / primer
 * render), para evitar parpadeos y errores de hidratación.
 */
export function useHasSession(): boolean | null {
  const [hasSession, setHasSession] = useState<boolean | null>(null)

  useEffect(() => {
    setHasSession(hasRecoverableSession())
  }, [])

  return hasSession
}
