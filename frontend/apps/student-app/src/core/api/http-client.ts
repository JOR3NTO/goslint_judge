/**
 * HttpApiAdapter — implementación concreta del IApiAdapter que habla con el backend real.
 *
 * Encapsula:
 * - La URL base de la API
 * - La lógica de autenticación (token en header Authorization)
 * - La serialización/deserialización JSON
 * - El manejo uniforme de errores HTTP
 *
 * Las features nunca importan esta clase directamente. Se inyecta a través de un
 * contexto o función factory, de forma que se pueda sustituir por MockApiAdapter en tests.
 */
import type { IApiAdapter, RequestOptions, ApiResponse } from "./types"
import { env } from "@/core/config/env"
import { getMockServer } from "./mock/server"
import {
  expireSession,
  getValidAccessToken,
  isPublicAuthPath,
  isSessionFailure,
  recoverFromSessionFailure,
} from "@/core/auth/session"
import { genericMessageForStatus, sanitizeErrorBody, sanitizeErrorMessage } from "./sanitize-error"

/**
 * Respuesta de error ya saneada: el cuerpo solo conserva textos seguros de primer nivel
 * y `error` nunca lleva tokens, cabeceras ni volcados crudos.
 */
function toErrorResponse<T>(status: number, rawBody: unknown): ApiResponse<T> {
  const body = sanitizeErrorBody(rawBody, status)
  const fallback = genericMessageForStatus(status)
  return {
    data: body as T,
    status,
    ok: false,
    error: sanitizeErrorMessage(body?.message ?? body?.error, fallback),
  }
}

export class HttpApiAdapter implements IApiAdapter {
  private readonly baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, "")
  }

  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
    options?: RequestOptions,
    retried = false
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${path}`
    // Gestión de sesión solo si la ruta no es de auth pública y el llamador no trae su
    // propia cabecera Authorization (p. ej. logout).
    const managed = !isPublicAuthPath(path) && !options?.headers?.Authorization
    // Comprueba `exp` antes de usar el token y lo renueva si hace falta.
    const token = managed ? await getValidAccessToken() : null

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    }

    let response: Response
    try {
      response = await fetch(url, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
        signal: options?.signal,
      })
    } catch (err) {
      // Una cancelación se propaga tal cual; un fallo de red se lanza sin detalles internos.
      if (err instanceof DOMException && err.name === "AbortError") throw err
      throw new Error(genericMessageForStatus(0))
    }

    // Sesión inválida (401) o 403 con un token ya caducado: se renueva una vez y se
    // reintenta; si no se puede, la sesión se cierra con aviso y se va al login.
    if (token && isSessionFailure(response.status, token)) {
      const fresh = retried ? null : await recoverFromSessionFailure(token)
      if (fresh) return this.request<T>(method, path, body, options, true)
      if (retried) expireSession()
    }

    let data: unknown
    try {
      data = await response.json()
    } catch {
      data = null
    }

    if (!response.ok) return toErrorResponse<T>(response.status, data)

    return { data: data as T, status: response.status, ok: true }
  }

  get<T>(path: string, options?: RequestOptions) {
    return this.request<T>("GET", path, undefined, options)
  }
  post<T>(path: string, body: unknown, options?: RequestOptions) {
    return this.request<T>("POST", path, body, options)
  }
  put<T>(path: string, body: unknown, options?: RequestOptions) {
    return this.request<T>("PUT", path, body, options)
  }
  patch<T>(path: string, body: unknown, options?: RequestOptions) {
    return this.request<T>("PATCH", path, body, options)
  }
  delete<T>(path: string, options?: RequestOptions) {
    return this.request<T>("DELETE", path, undefined, options)
  }
}

/**
 * Adapter compartido por las features, apuntando a `env.API_BASE_URL`.
 *
 * Con `NEXT_PUBLIC_USE_MOCK=true` devuelve el MockServer (singleton) en lugar del backend,
 * salvo en producción: ahí el mock se ignora, se avisa en consola y se usa el backend real.
 * Con IS_MOCK en false el comportamiento es el de siempre.
 */
let warnedMockInProduction = false

export function createHttpApi(): IApiAdapter {
  if (env.IS_MOCK) {
    if (process.env.NODE_ENV === "production") {
      if (!warnedMockInProduction) {
        warnedMockInProduction = true
        console.warn(
          "[goslint] NEXT_PUBLIC_USE_MOCK=true se ignora en producción: se usa el backend real."
        )
      }
    } else {
      return getMockServer()
    }
  }
  return new HttpApiAdapter(env.API_BASE_URL)
}
