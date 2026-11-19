/**
 * Utilidades puras para leer JWT en el cliente.
 *
 * Solo DECODIFICAN el payload: la firma no se verifica nunca aquí. Sirven para decidir
 * en el navegador si una sesión ya caducó (claim `exp`) antes de usarla; quien autoriza
 * de verdad es siempre el backend.
 */

/** Margen por defecto: un token al que le quedan menos de 30 s se trata como caducado. */
export const DEFAULT_EXPIRY_SKEW_MS = 30_000

/** Decodifica el payload (segunda parte) de un JWT. `null` si el token está mal formado. */
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split(".")
    if (parts.length !== 3 || !parts[1]) return null
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/")
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=")
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes))
    return claims && typeof claims === "object" && !Array.isArray(claims)
      ? (claims as Record<string, unknown>)
      : null
  } catch {
    return null
  }
}

/** Claim `exp` en milisegundos epoch, o `null` si falta o el token no se puede leer. */
export function getJwtExpiryMs(token: string): number | null {
  const exp = decodeJwtPayload(token)?.exp
  return typeof exp === "number" && Number.isFinite(exp) ? exp * 1000 : null
}

/**
 * `true` si el token ya caducó (o caduca dentro de `skewMs`), o si no se puede leer.
 * Un token sin `exp` legible se considera inservible: el backend siempre lo emite.
 */
export function isJwtExpired(
  token: string,
  nowMs: number = Date.now(),
  skewMs: number = DEFAULT_EXPIRY_SKEW_MS
): boolean {
  const expMs = getJwtExpiryMs(token)
  if (expMs === null) return true
  return expMs - skewMs <= nowMs
}
