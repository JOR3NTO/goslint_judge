/**
 * Sesiones del mock: emite JWT FALSOS bien formados (header.payload.firma) para que
 * `decodeJwt` y `readContestSession` funcionen, y los verifica igual que el backend.
 *
 * La firma es la cadena literal "mock-signature". Un verificador real la rechaza, así que
 * estos tokens no sirven contra ningún servicio. Llevan además `mock: true` en el payload.
 */
import type { UserRecord, MockState } from "./model"
import { ACCESS_TOKEN_TTL_MS, MOCK_SIGNATURE, REFRESH_TOKEN_TTL_MS } from "./model"
import { base64UrlDecode, base64UrlEncode, newId } from "./util"
import { httpError } from "./router"

export interface AccessClaims {
  sub: string
  email: string
  role: UserRecord["role"]
  iss: string
  iat: number
  exp: number
  jti: string
  mock: true
}

export interface IssuedSession {
  accessToken: string
  refreshToken: string
  type: "Bearer"
  /** Duración del access token en milisegundos (como LoginResponse). */
  expiresIn: number
}

/** Crea access + refresh token para el usuario y registra el refresh token en el estado. */
export function issueSession(state: MockState, user: UserRecord, now: number): IssuedSession {
  const jti = newId()
  const claims: AccessClaims = {
    sub: user.id,
    email: user.email,
    role: user.role,
    iss: "goslint-mock",
    iat: Math.floor(now / 1000),
    exp: Math.floor((now + ACCESS_TOKEN_TTL_MS) / 1000),
    jti,
    mock: true,
  }
  const header = base64UrlEncode(JSON.stringify({ alg: "HS256", typ: "JWT" }))
  const payload = base64UrlEncode(JSON.stringify(claims))
  const accessToken = `${header}.${payload}.${MOCK_SIGNATURE}`

  const refreshToken = newId()
  state.refreshTokens = state.refreshTokens.filter((r) => r.expiresAt > now)
  state.refreshTokens.push({ token: refreshToken, userId: user.id, expiresAt: now + REFRESH_TOKEN_TTL_MS })

  return { accessToken, refreshToken, type: "Bearer", expiresIn: ACCESS_TOKEN_TTL_MS }
}

/** Extrae el token de `Authorization: Bearer <token>`. */
export function bearerToken(authorization: string | null): string | null {
  const match = /^Bearer\s+(\S+)$/i.exec(authorization ?? "")
  return match ? match[1] : null
}

/** Decodifica las claims de un token del mock. `null` si el token no tiene el formato esperado. */
export function readClaims(token: string): AccessClaims | null {
  const parts = token.split(".")
  if (parts.length !== 3) return null
  try {
    const claims = JSON.parse(base64UrlDecode(parts[1])) as AccessClaims
    return claims && typeof claims.sub === "string" ? claims : null
  } catch {
    return null
  }
}

/**
 * Valida el access token de la cabecera y devuelve el usuario.
 * Lanza 401 con los mismos mensajes que ProfileService del backend.
 */
export function authenticate(state: MockState, authorization: string | null, now: number): UserRecord {
  const token = bearerToken(authorization)
  if (!token) throw httpError(401, "Token no proporcionado o formato inválido", "error")

  const parts = token.split(".")
  if (parts.length !== 3 || parts[2] !== MOCK_SIGNATURE) {
    throw httpError(401, "Token inválido o expirado", "error")
  }
  const claims = readClaims(token)
  if (!claims || claims.mock !== true || claims.exp * 1000 <= now) {
    throw httpError(401, "Token inválido o expirado", "error")
  }
  if (state.revokedSessions.includes(claims.jti)) {
    throw httpError(401, "Sesión cerrada o token revocado", "error")
  }
  const user = state.users.find((u) => u.id === claims.sub)
  if (!user || !user.isActive) throw httpError(401, "Usuario no encontrado", "error")
  return user
}

/** Logout: revoca el access token (si es válido de formato) y los refresh tokens del usuario. */
export function revokeSession(state: MockState, authorization: string | null): void {
  const token = bearerToken(authorization)
  const claims = token ? readClaims(token) : null
  if (!claims) return
  if (!state.revokedSessions.includes(claims.jti)) state.revokedSessions.push(claims.jti)
  state.refreshTokens = state.refreshTokens.filter((r) => r.userId !== claims.sub)
}
