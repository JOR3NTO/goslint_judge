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

export class HttpApiAdapter implements IApiAdapter {
  private readonly baseUrl: string

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, "")
  }

  private getAuthHeaders(): Record<string, string> {
    // TODO: obtener token del store de auth cuando se implemente
    const token = typeof window !== "undefined" ? localStorage.getItem("auth_token") : null
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  private async request<T>(
    method: string,
    path: string,
    body?: unknown,
    options?: RequestOptions
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${path}`
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...this.getAuthHeaders(),
      ...(options?.headers ?? {}),
    }

    const response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: options?.signal,
    })

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
