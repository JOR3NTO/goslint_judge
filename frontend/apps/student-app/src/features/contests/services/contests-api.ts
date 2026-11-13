import { createHttpApi } from "@/core/api"
import type { IApiAdapter } from "@/core/api"
import { ContestsService } from "./contests-service"

/**
 * Adapter HTTP de la feature `contests`.
 * Las rutas que usa `ContestsService` son relativas (`/v1/...`): `HttpApiAdapter`
 * antepone la base `/api` y los rewrites de next.config.mjs las envían al contest-service.
 */
export function createContestsApi(): IApiAdapter {
  return createHttpApi()
}

/** Servicio listo para usar desde los hooks de la feature. */
export function createContestsService(): ContestsService {
  return new ContestsService(createContestsApi())
}
