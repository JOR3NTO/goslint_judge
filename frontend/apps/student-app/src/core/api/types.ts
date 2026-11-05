/**
 * IApiAdapter — interfaz de dominio estable que consumen las features.
 * Las features solo dependen de esta interfaz, nunca de la implementación concreta.
 * Esto permite intercambiar la fuente de datos (REST, GraphQL, mock, etc.) sin tocar
 * ni una línea de código de las features.
 */
export interface IApiAdapter {
  get<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>>
  post<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  put<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  patch<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>>
}

export interface RequestOptions {
  headers?: Record<string, string>
  signal?: AbortSignal
}

export interface ApiResponse<T> {
  data: T
  status: number
  ok: boolean
  error?: string
}

export interface ApiError {
  message: string
  status: number
  code?: string
}
