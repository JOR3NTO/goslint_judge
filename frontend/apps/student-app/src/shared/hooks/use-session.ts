"use client"

import { useEffect, useState } from "react"

/** Clave de localStorage donde `use-login` guarda el access token. */
export const AUTH_TOKEN_KEY = "auth_token"

/**
 * Indica si hay un access token guardado en el navegador.
 *
 * `null` mientras no se ha leído localStorage (render de servidor / primer
 * render), para evitar parpadeos y errores de hidratación.
 */
export function useHasSession(): boolean | null {
  const [hasSession, setHasSession] = useState<boolean | null>(null)

  useEffect(() => {
    setHasSession(Boolean(localStorage.getItem(AUTH_TOKEN_KEY)))
  }, [])

  return hasSession
}
