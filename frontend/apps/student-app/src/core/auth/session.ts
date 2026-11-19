/**
 * Sesión del estudiante en el navegador: único sitio que lee y escribe los tokens.
 *
 * - Antes de usar el access token se mira su `exp` (sin verificar la firma). Si caducó
 *   se intenta UNA renovación con `POST /v1/auth/refresh`, compartida por todas las
 *   peticiones concurrentes de la pestaña.
 * - Si no se puede renovar, la sesión se cierra limpiamente: se borran los tokens, se
 *   deja el aviso "Tu sesión expiró, vuelve a iniciar sesión" y se manda a /login
 *   (nunca se redirige si ya se está en /login, así no hay bucles).
 *
 * Lo usan HttpApiAdapter, el MockServer, el WebSocket de envíos, el guard de rutas
 * protegidas, use-login y use-logout.
 */
import { env } from "@/core/config/env"
import { isJwtExpired } from "./jwt"

/** Claves de localStorage (mismas que usaba use-login). */
export const AUTH_TOKEN_KEY = "auth_token"
export const REFRESH_TOKEN_KEY = "refresh_token"
/** Aviso pendiente para la pantalla de login (sessionStorage, un solo uso). */
const SESSION_NOTICE_KEY = "session_notice"

export const LOGIN_PATH = "/login"
export const SESSION_EXPIRED_MESSAGE = "Tu sesión expiró, vuelve a iniciar sesión"

/** Endpoints de auth que nunca llevan el token guardado ni disparan la gestión de sesión. */
export const PUBLIC_AUTH_PATHS = [
  "/v1/auth/login",
  "/v1/auth/register",
  "/v1/auth/refresh",
  "/v1/auth/check-username",
  "/v1/auth/logout",
]

export function isPublicAuthPath(path: string): boolean {
  const clean = path.split("?")[0]
  return PUBLIC_AUTH_PATHS.includes(clean)
}

function hasWindow(): boolean {
  return typeof window !== "undefined"
}

// ─── Almacenamiento ──────────────────────────────────────────────────────────

export function readAccessToken(): string | null {
  if (!hasWindow()) return null
  try {
    return localStorage.getItem(AUTH_TOKEN_KEY)
  } catch {
    return null
  }
}

function readRefreshToken(): string | null {
  if (!hasWindow()) return null
  try {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  } catch {
    return null
  }
}

/** Guarda el par de tokens tras login o refresh. */
export function storeTokens(accessToken: string, refreshToken?: string | null): void {
  if (!hasWindow()) return
  try {
    localStorage.setItem(AUTH_TOKEN_KEY, accessToken)
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } catch {
    // Almacenamiento bloqueado: la sesión dura lo que dure la pestaña.
  }
  redirecting = false
}

/** Borra los tokens sin redirigir (logout voluntario). */
export function clearTokens(): void {
  if (!hasWindow()) return
  try {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    // Nada que limpiar.
  }
}

/** `true` si hay un access token vigente o, si caducó, un refresh token con el que intentarlo. */
export function hasRecoverableSession(): boolean {
  const token = readAccessToken()
  if (!token) return false
  return !isJwtExpired(token) || Boolean(readRefreshToken())
}

// ─── Aviso para el login ─────────────────────────────────────────────────────

/** Lee y borra el aviso pendiente (lo muestra la pantalla de login). */
export function consumeSessionNotice(): string | null {
  if (!hasWindow()) return null
  try {
    const notice = sessionStorage.getItem(SESSION_NOTICE_KEY)
    if (notice) sessionStorage.removeItem(SESSION_NOTICE_KEY)
    return notice
  } catch {
    return null
  }
}

// ─── Cierre por expiración ───────────────────────────────────────────────────

let redirecting = false

function isOnLoginPage(): boolean {
  const path = window.location.pathname.replace(/\/+$/, "")
  return path === LOGIN_PATH || path.startsWith(`${LOGIN_PATH}/`)
}

/**
 * Sesión caducada o inválida: borra los tokens, deja el aviso y manda a /login una sola
 * vez. En /login no redirige (evita bucles y no molesta al formulario).
 */
export function expireSession(): void {
  if (!hasWindow()) return
  clearTokens()
  if (redirecting || isOnLoginPage()) return
  redirecting = true
  try {
    sessionStorage.setItem(SESSION_NOTICE_KEY, SESSION_EXPIRED_MESSAGE)
  } catch {
    // Sin sessionStorage el login se muestra sin aviso.
  }
  window.location.assign(LOGIN_PATH)
}

// ─── Renovación ──────────────────────────────────────────────────────────────

interface TokenPair {
  accessToken: string
  refreshToken?: string
}

function isTokenPair(value: unknown): value is TokenPair {
  return (
    !!value &&
    typeof value === "object" &&
    typeof (value as TokenPair).accessToken === "string" &&
    (value as TokenPair).accessToken.length > 0
  )
}

/** Llama a `POST /v1/auth/refresh` (backend real o mock). `null` si no se pudo renovar. */
async function callRefreshEndpoint(refreshToken: string): Promise<TokenPair | null> {
  try {
    if (env.MOCK_ENABLED) {
      const { getMockServer } = await import("@/core/api/mock/server")
      const res = await getMockServer().post<unknown>("/v1/auth/refresh", { refreshToken })
      return res.ok && isTokenPair(res.data) ? res.data : null
    }
    const res = await fetch(`${env.API_BASE_URL.replace(/\/$/, "")}/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const data: unknown = await res.json().catch(() => null)
    return isTokenPair(data) ? data : null
  } catch {
    return null
  }
}

let refreshInFlight: Promise<string | null> | null = null

/**
 * Renueva el access token. Todas las llamadas simultáneas comparten la misma petición
 * (el backend rota el refresh token: dos refresh en paralelo invalidarían uno de ellos).
 * Devuelve el nuevo access token o `null`; NO cierra la sesión por sí misma.
 */
export function refreshSession(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight
  refreshInFlight = (async () => {
    const refreshToken = readRefreshToken()
    if (!refreshToken) return null
    const pair = await callRefreshEndpoint(refreshToken)
    if (pair) {
      storeTokens(pair.accessToken, pair.refreshToken)
      return pair.accessToken
    }
    // Otra pestaña pudo renovar a la vez (rotación): si dejó un token vigente, se usa.
    const current = readAccessToken()
    return current && !isJwtExpired(current) ? current : null
  })().finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

/**
 * Access token listo para usar, renovándolo si caducó.
 * - Sin sesión guardada: `null` (visitante anónimo; no se redirige).
 * - Sesión caducada y no renovable: cierra la sesión (aviso + /login) y devuelve `null`.
 */
export async function getValidAccessToken(): Promise<string | null> {
  const token = readAccessToken()
  if (!token) return null
  if (!isJwtExpired(token)) return token
  const fresh = await refreshSession()
  if (fresh) return fresh
  expireSession()
  return null
}

/**
 * ¿Esta respuesta indica que la sesión con la que se hizo la petición ya no sirve?
 * 401 siempre; 403 solo si el token enviado ya caducó (el backend actual responde 403
 * a un token caducado porque lo trata como anónimo).
 */
export function isSessionFailure(status: number, sentToken: string): boolean {
  return status === 401 || (status === 403 && isJwtExpired(sentToken))
}

/**
 * Tras una respuesta de sesión fallida: intenta renovar (una vez) y devuelve el token
 * nuevo para reintentar, o cierra la sesión y devuelve `null`.
 */
export async function recoverFromSessionFailure(sentToken: string): Promise<string | null> {
  // Si otra petición ya renovó mientras esta viajaba, basta con reintentar con el nuevo.
  const current = readAccessToken()
  if (current && current !== sentToken && !isJwtExpired(current)) return current
  const fresh = await refreshSession()
  if (fresh && fresh !== sentToken) return fresh
  expireSession()
  return null
}
