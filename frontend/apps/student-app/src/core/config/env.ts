/**
 * Configuración de entorno de la aplicación.
 * Lee variables de entorno de Next.js (NEXT_PUBLIC_*) con fallbacks seguros.
 */
export const env = {
  API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api",
  WS_URL:       process.env.NEXT_PUBLIC_WS_URL ?? "/ws",
  /** Landing pública (landing-app). El logo de la app y la ruta "/" apuntan aquí. */
  LANDING_URL:  process.env.NEXT_PUBLIC_LANDING_URL ?? "http://localhost:3002",
  /** WebSocket directo a submission-service (los rewrites de Next no proxean WS). */
  SUBMISSION_WS_URL: process.env.NEXT_PUBLIC_SUBMISSION_WS_URL ?? "ws://localhost:8080/ws/submissions",
  /** `NEXT_PUBLIC_USE_MOCK=true`. Por sí solo no basta: en producción el mock se ignora (ver `MOCK_ENABLED`). */
  IS_MOCK:      process.env.NEXT_PUBLIC_USE_MOCK === "true",
  /** Mock realmente activo: IS_MOCK y nunca en producción. Usar este flag en la UI (banner, botón demo, sin WebSocket). */
  MOCK_ENABLED: process.env.NEXT_PUBLIC_USE_MOCK === "true" && process.env.NODE_ENV !== "production",
  NODE_ENV:     process.env.NODE_ENV ?? "development",
} as const
