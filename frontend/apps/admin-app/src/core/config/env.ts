/**
 * Configuración de entorno de la aplicación.
 * Lee variables de entorno de Next.js (NEXT_PUBLIC_*) con fallbacks seguros.
 */
export const env = {
  API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
  WS_URL:       process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws",
  IS_MOCK:      process.env.NEXT_PUBLIC_USE_MOCK === "true",
  NODE_ENV:     process.env.NODE_ENV ?? "development",
} as const
