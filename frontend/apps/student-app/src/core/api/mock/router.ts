/**
 * Enrutador del mock: patrones tipo "/v1/contests/:id/teams", extracción de params,
 * acceso al body y al query string, y errores HTTP tipados.
 */
import type { MockState } from "./model"

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE"

export interface MockRequest {
  method: HttpMethod
  /** Ruta sin query string. */
  path: string
  params: Record<string, string>
  query: URLSearchParams
  body: unknown
  /** Valor de la cabecera Authorization (`Bearer <token>`) o `null`. */
  authorization: string | null
}

export interface MockContext {
  state: MockState
  /** Instante actual (epoch ms). Inyectable para pruebas. */
  now: number
}

export interface MockResponse {
  status: number
  body?: unknown
}

export type RouteHandler = (req: MockRequest, ctx: MockContext) => MockResponse

/**
 * Error que las rutas lanzan para responder con un código HTTP.
 * `key` decide el nombre del campo del cuerpo: `{ message }` en los servicios Java con
 * ErrorResponseDTO y `{ error }` en auth-service.
 */
export class MockHttpError extends Error {
  readonly status: number
  readonly body: Record<string, unknown>

  constructor(status: number, body: Record<string, unknown>) {
    const firstText = Object.values(body).find((v): v is string => typeof v === "string")
    super(firstText ?? `Error HTTP ${status}`)
    this.name = "MockHttpError"
    this.status = status
    this.body = body
  }
}

export function httpError(status: number, message: string, key: "message" | "error" = "message"): MockHttpError {
  return new MockHttpError(status, { [key]: message })
}

/** Errores de validación por campo (400 con cuerpo `{ campo: "mensaje" }`, como auth-service). */
export function fieldErrors(fields: Record<string, string>): MockHttpError {
  return new MockHttpError(400, fields)
}

export function ok(body?: unknown): MockResponse {
  return { status: 200, body }
}

export function created(body: unknown): MockResponse {
  return { status: 201, body }
}

export function noContent(): MockResponse {
  return { status: 204 }
}

/** Devuelve el body como objeto o lanza 400 si no es un JSON de objeto. */
export function readBody(req: MockRequest, key: "message" | "error" = "message"): Record<string, unknown> {
  if (req.body && typeof req.body === "object" && !Array.isArray(req.body)) {
    return req.body as Record<string, unknown>
  }
  throw httpError(400, "El cuerpo de la petición no es un JSON válido o tiene un valor no permitido.", key)
}

/** Lee un campo de texto del body; `undefined` si no existe o no es string. */
export function textField(body: Record<string, unknown>, name: string): string | undefined {
  const value = body[name]
  return typeof value === "string" ? value : undefined
}

interface Route {
  method: HttpMethod
  pattern: string
  segments: string[]
  handler: RouteHandler
}

export type RouteResolution =
  | { kind: "match"; handler: RouteHandler; params: Record<string, string> }
  | { kind: "method-not-allowed"; allowed: HttpMethod[] }
  | { kind: "not-found" }

function splitPath(path: string): string[] {
  return path.split("/").filter((segment) => segment.length > 0)
}

export class ApiRouter {
  private readonly routes: Route[] = []

  add(method: HttpMethod, pattern: string, handler: RouteHandler): void {
    this.routes.push({ method, pattern, segments: splitPath(pattern), handler })
  }

  resolve(method: HttpMethod, path: string): RouteResolution {
    const requested = splitPath(path)
    const allowed: HttpMethod[] = []

    for (const route of this.routes) {
      const params = matchSegments(route.segments, requested)
      if (!params) continue
      if (route.method === method) return { kind: "match", handler: route.handler, params }
      allowed.push(route.method)
    }

    return allowed.length > 0 ? { kind: "method-not-allowed", allowed } : { kind: "not-found" }
  }
}

function matchSegments(pattern: string[], requested: string[]): Record<string, string> | null {
  if (pattern.length !== requested.length) return null
  const params: Record<string, string> = {}
  for (let i = 0; i < pattern.length; i++) {
    const part = pattern[i]
    const value = requested[i]
    if (part.startsWith(":")) {
      params[part.slice(1)] = decodeURIComponent(value)
    } else if (part !== value) {
      return null
    }
  }
  return params
}
