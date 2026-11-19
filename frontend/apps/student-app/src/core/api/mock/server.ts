/**
 * MockServer — implementación de IApiAdapter que responde con datos simulados.
 *
 * Es un servidor en memoria con enrutador, estado mutable y latencia simulada. Devuelve
 * `{ data, status, ok, error }` igual que HttpApiAdapter, y en los errores reproduce
 * el comportamiento de sesión caducada (borra tokens y manda a /login).
 *
 * Solo se obtiene a través de `createHttpApi()` (core/api/http-client.ts), que lo activa
 * con NEXT_PUBLIC_USE_MOCK=true fuera de producción.
 */
import type { ApiResponse, IApiAdapter, RequestOptions } from "../types"
import { ApiRouter, MockHttpError, type HttpMethod, type MockResponse } from "./router"
import { registerAuthRoutes } from "./routes-auth"
import { registerContestRoutes } from "./routes-contests"
import { registerTeamRoutes } from "./routes-teams"
import { registerProblemRoutes } from "./routes-problems"
import { registerSubmissionRoutes } from "./routes-submissions"
import { clearStoredState, defaultStorage, loadState, saveState, type StorageLike } from "./db"
import type { MockState } from "./model"
import { sleep } from "./util"
import {
  clearTokens,
  expireSession,
  getValidAccessToken,
  isPublicAuthPath,
  isSessionFailure,
  recoverFromSessionFailure,
} from "@/core/auth/session"
import { genericMessageForStatus, sanitizeErrorBody, sanitizeErrorMessage } from "../sanitize-error"

export interface MockServerOptions {
  /** Reloj inyectable (epoch ms). Por defecto `Date.now`. */
  now?: () => number
  /** Latencia simulada en ms. `null` la desactiva (pruebas). */
  latency?: { minMs: number; maxMs: number } | null
  /** Almacenamiento del estado. `null` = solo memoria. Por defecto sessionStorage. */
  storage?: StorageLike | null
}

const STATUS_TEXT: Record<number, string> = {
  400: "Bad Request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not Found",
  405: "Method Not Allowed",
  409: "Conflict",
  423: "Locked",
  500: "Internal Server Error",
}

function errorText(body: unknown): string | undefined {
  if (!body || typeof body !== "object") return undefined
  const record = body as Record<string, unknown>
  if (typeof record.message === "string") return record.message
  if (typeof record.error === "string") return record.error
  return Object.values(record).find((v): v is string => typeof v === "string")
}

function toApiResponse<T>(response: MockResponse): ApiResponse<T> {
  if (response.status >= 200 && response.status < 300) {
    return { data: (response.body ?? null) as T, status: response.status, ok: true }
  }
  // Mismo saneado que HttpApiAdapter: solo textos seguros de primer nivel.
  const body = sanitizeErrorBody(response.body, response.status)
  const fallback = STATUS_TEXT[response.status] ? genericMessageForStatus(response.status) : `Error ${response.status}`
  return {
    data: body as T,
    status: response.status,
    ok: false,
    error: sanitizeErrorMessage(errorText(body), fallback),
  }
}

export class MockServer implements IApiAdapter {
  private readonly router = new ApiRouter()
  private readonly now: () => number
  private readonly latency: { minMs: number; maxMs: number } | null
  private readonly storage: StorageLike | null
  private state: MockState

  constructor(options: MockServerOptions = {}) {
    this.now = options.now ?? (() => Date.now())
    this.latency = options.latency === undefined ? { minMs: 150, maxMs: 400 } : options.latency
    this.storage = options.storage === undefined ? defaultStorage() : options.storage

    registerAuthRoutes(this.router)
    registerContestRoutes(this.router)
    registerTeamRoutes(this.router)
    registerProblemRoutes(this.router)
    registerSubmissionRoutes(this.router)

    this.state = loadState(this.storage, this.now())
  }

  get<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>("GET", path, undefined, options)
  }
  post<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>("POST", path, body, options)
  }
  put<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>("PUT", path, body, options)
  }
  patch<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>("PATCH", path, body, options)
  }
  delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>("DELETE", path, undefined, options)
  }

  private async request<T>(
    method: HttpMethod,
    rawPath: string,
    body: unknown,
    options?: RequestOptions,
    retried = false
  ): Promise<ApiResponse<T>> {
    if (this.latency) {
      const { minMs, maxMs } = this.latency
      await sleep(minMs + Math.random() * (maxMs - minMs))
    }

    const queryIndex = rawPath.indexOf("?")
    const path = queryIndex === -1 ? rawPath : rawPath.slice(0, queryIndex)
    const query = new URLSearchParams(queryIndex === -1 ? "" : rawPath.slice(queryIndex + 1))

    // Mismo criterio que HttpApiAdapter: el token guardado (comprobado su `exp` y renovado
    // si hace falta) solo va en rutas no públicas y si el llamador no trae su cabecera.
    const managed = !isPublicAuthPath(path) && !options?.headers?.Authorization
    const storedToken = managed ? await getValidAccessToken() : null
    const headers: Record<string, string> = {
      ...(storedToken ? { Authorization: `Bearer ${storedToken}` } : {}),
      ...(options?.headers ?? {}),
    }
    const authorization = headers.Authorization ?? null

    const response = this.dispatch(method, path, query, body, authorization)
    saveState(this.storage, this.state)

    // Sesión inválida: igual que HttpApiAdapter, se renueva una vez o se cierra con aviso.
    if (storedToken && isSessionFailure(response.status, storedToken)) {
      const fresh = retried ? null : await recoverFromSessionFailure(storedToken)
      if (fresh) return this.request<T>(method, rawPath, body, options, true)
      if (retried) expireSession()
    }

    return toApiResponse<T>(response)
  }

  private dispatch(
    method: HttpMethod,
    path: string,
    query: URLSearchParams,
    body: unknown,
    authorization: string | null
  ): MockResponse {
    const resolution = this.router.resolve(method, path)

    if (resolution.kind === "not-found") {
      console.warn(`[mock] Ruta no simulada: ${method} ${path}`)
      return { status: 404, body: { message: `Ruta no disponible en el modo demo: ${method} ${path}` } }
    }
    if (resolution.kind === "method-not-allowed") {
      return { status: 405, body: { message: `Método ${method} no permitido en ${path}` } }
    }

    try {
      return resolution.handler(
        { method, path, params: resolution.params, query, body, authorization },
        { state: this.state, now: this.now() }
      )
    } catch (error) {
      if (error instanceof MockHttpError) {
        return { status: error.status, body: error.body }
      }
      console.error("[mock] Error inesperado en el modo demo:", error)
      return { status: 500, body: { message: "Error interno del modo demo." } }
    }
  }
}

// ─── Instancia compartida ────────────────────────────────────────────────────

let sharedServer: MockServer | null = null

/** Servidor mock único del navegador (las features comparten el mismo estado). */
export function getMockServer(): MockServer {
  if (!sharedServer) sharedServer = new MockServer()
  return sharedServer
}

/**
 * Borra los datos demo (sessionStorage) y la sesión local. La siguiente llamada
 * siembra de nuevo. Quien lo use debe recargar la página después.
 */
export function resetMockData(): void {
  clearStoredState(defaultStorage())
  clearTokens()
  sharedServer = null
}
