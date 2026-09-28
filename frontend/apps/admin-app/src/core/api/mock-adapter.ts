/**
 * MockApiAdapter — implementación alternativa del IApiAdapter para desarrollo y pruebas.
 *
 * Permite trabajar en el frontend SIN un backend activo: registra handlers por path+método
 * y devuelve datos estáticos o generados dinámicamente.
 *
 * Uso:
 *   const adapter = new MockApiAdapter()
 *   adapter.register("GET", "/contests", () => ({ data: mockContests }))
 *
 * Para agregar un nuevo mock, llama a adapter.register() antes de usarlo en el proveedor.
 */
import type { IApiAdapter, RequestOptions, ApiResponse } from "./types"

type MockHandler<T> = () => Omit<ApiResponse<T>, "status" | "ok"> & { status?: number }

export class MockApiAdapter implements IApiAdapter {
  private handlers = new Map<string, MockHandler<unknown>>()

  register<T>(method: string, path: string, handler: MockHandler<T>): this {
    this.handlers.set(`${method.toUpperCase()}:${path}`, handler as MockHandler<unknown>)
    return this
  }

  private async handle<T>(method: string, path: string): Promise<ApiResponse<T>> {
    const key = `${method}:${path}`
    const handler = this.handlers.get(key)

    if (!handler) {
      console.warn(`[MockApiAdapter] No handler registered for ${key}`)
      return { data: null as T, status: 404, ok: false, error: "Not found in mock" }
    }

    await new Promise((resolve) => setTimeout(resolve, 200)) // Simula latencia de red
    const result = handler() as Omit<ApiResponse<T>, "ok">
    return { ...result, status: result.status ?? 200, ok: true }
  }

  get<T>(path: string, _options?: RequestOptions) {
    return this.handle<T>("GET", path)
  }
  post<T>(path: string, _body: unknown, _options?: RequestOptions) {
    return this.handle<T>("POST", path)
  }
  put<T>(path: string, _body: unknown, _options?: RequestOptions) {
    return this.handle<T>("PUT", path)
  }
  patch<T>(path: string, _body: unknown, _options?: RequestOptions) {
    return this.handle<T>("PATCH", path)
  }
  delete<T>(path: string, _options?: RequestOptions) {
    return this.handle<T>("DELETE", path)
  }
}
