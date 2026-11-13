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

/** Clave de localStorage del access token (la escribe use-login). */
const AUTH_TOKEN_KEY = "auth_token"
/** Clave de localStorage del refresh token (la escribe use-login). */
const REFRESH_TOKEN_KEY = "refresh_token"
/** Ruta a la que se manda al usuario cuando su sesión deja de ser válida. */
const LOGIN_PATH = "/login"

/**
 * Endpoints de auth que van sin token: login y registro no tienen sesión todavía,
 * y un 401 ahí significa credenciales incorrectas, no sesión caducada.
 */
const PUBLIC_AUTH_PATHS = [
  "/v1/auth/login",
  "/v1/auth/register",
  "/v1/auth/refresh",
  "/v1/auth/check-username",
]

function readStoredToken(): string | null {
  if (typeof window === "undefined") return null
  try {
    return localStorage.getItem(AUTH_TOKEN_KEY)
  } catch {
    return null
  }
}

function clearSession(): void {
  if (typeof window === "undefined") return
  try {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    // Almacenamiento bloqueado (modo privado, etc.): nada que limpiar.
  }
}

export class HttpApiAdapter implements IApiAdapter {
  private readonly baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, "")
  }

  private isPublicPath(path: string): boolean {
    return PUBLIC_AUTH_PATHS.some((p) => path === p || path.startsWith(`${p}?`))
  }

  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${path}`
    const isPublic = this.isPublicPath(path)
    const token = isPublic ? null : readStoredToken()

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    }

    const response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: options?.signal,
    })

    // Sesión caducada o revocada: se borra el token y se manda al login.
    // Solo si la petición llevaba token; un 401 sin token no es sesión caducada.
    if (response.status === 401 && token && typeof window !== "undefined") {
      clearSession()
      window.location.assign(LOGIN_PATH)
    }

    let data: T
    try {
      data = await response.json()
    } catch {
      data = null as T
    }

    if (!response.ok) {
      return {
        data,
        status: response.status,
        ok: false,
        error: (data as { message?: string })?.message ?? response.statusText,
      }
    }

    return { data, status: response.status, ok: true }
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
