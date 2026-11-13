"use client"

import { AUTH_TOKEN_KEY } from "@/shared/hooks/use-session"

export interface ContestSession {
  token: string
  /** Claim `sub` del JWT: id del usuario. */
  userId: string
}

/**
 * Lee la sesión guardada por use-login. Devuelve `null` si no hay token,
 * si localStorage no está disponible o si el JWT está mal formado.
 *
 * La firma NO se verifica: solo se usa para pedir datos al backend, que
 * es quien autoriza. Misma lógica que features/profile/utils/decode-jwt.ts
 * (no se importa desde allí para respetar el encapsulamiento de la feature).
 */
export function readContestSession(): ContestSession | null {
  let token: string | null = null
  try {
    token = localStorage.getItem(AUTH_TOKEN_KEY)
  } catch {
    return null
  }
  if (!token) return null

  const userId = readSubClaim(token)
  return userId ? { token, userId } : null
}

function readSubClaim(token: string): string | null {
  try {
    const payload = token.split(".")[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/")
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=")
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    const claims = JSON.parse(new TextDecoder().decode(bytes)) as { sub?: unknown }
    return typeof claims.sub === "string" && claims.sub ? claims.sub : null
  } catch {
    return null
  }
}
