/**
 * Variables de entorno de landing-app (ver .env.example).
 * Son NEXT_PUBLIC_*: se leen en el build y se exponen al navegador.
 *
 * APP_URL → URL de student-app (login, register). Default: http://localhost:3000
 * API_URL → URL base del backend contest-platform (GET /api/v1/contests, público).
 *           Default: http://localhost:8080 (puerto por defecto de contest-platform)
 */
export const env = {
  /** URL base de student-app. Los CTAs de la landing apuntan aquí. */
  APP_URL: process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000",
  /** URL base del backend contest-platform. La landing consume /api/v1/contests. */
  API_URL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
} as const
