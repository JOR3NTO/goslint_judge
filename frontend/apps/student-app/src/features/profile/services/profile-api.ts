import { createHttpApi } from "@/core/api"
import type { IApiAdapter } from "@/core/api"

/**
 * Adapter de la feature `profile`. Con el modo mock activo, `createHttpApi()` devuelve el
 * MockServer, que simula `GET /v1/auth/me` a partir del usuario de la sesión.
 */
export function createProfileApi(): IApiAdapter {
  return createHttpApi()
}
