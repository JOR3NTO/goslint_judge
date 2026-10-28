import type { JwtClaims } from "../types/profile.types"

/**
 * Decodifica el payload de un JWT SIN verificar la firma.
 *
 * Solo se usa para mostrar datos en la UI; la autorización real la hace
 * siempre el backend. Devuelve `null` si el token está mal formado.
 */
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split(".")[1]
    if (!payload) return null

    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/")
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=")
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    const json = new TextDecoder().decode(bytes)
    const claims = JSON.parse(json) as JwtClaims

    if (!claims.sub || !claims.email || !claims.role) return null
    return claims
  } catch {
    return null
  }
}
