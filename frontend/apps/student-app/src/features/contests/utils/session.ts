"use client"

import { hasRecoverableSession, readAccessToken } from "@/core/auth/session"

export interface ContestSession {
  token: string
  /** Claim `sub` del JWT: id del usuario. */
  userId: string
}

/**
 * Lee la sesión guardada por use-login. Devuelve `null` si no hay token, si
 * localStorage no está disponible, si el JWT está mal formado o si ya caducó y no
 * hay refresh token con el que renovarlo (sesión irrecuperable).
 *
 * Si el access token caducó pero hay refresh token, se devuelve igual: `userId` no
 * cambia al renovar, y quien llame al backend debe pedir el token con
 * `getValidAccessToken()` (core/auth/session), que lo renueva. No uses `token` de aquí
 * para abrir conexiones: puede estar caducado.
 *
 * La firma NO se verifica: solo se usa para pedir datos al backend, que
 * es quien autoriza. Misma lógica que features/profile/utils/decode-jwt.ts
 * (no se importa desde allí para respetar el encapsulamiento de la feature).
 */
export function readContestSession(): ContestSession | null {
  if (!hasRecoverableSession()) return null
  const token = readAccessToken()
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
