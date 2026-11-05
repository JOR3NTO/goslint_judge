/**
 * Variables de entorno de landing-app.
 * APP_URL → URL de student-app (login, register, judge)
 * API_URL → URL del backend (endpoints públicos)
 */
export const env = {
  /** URL base de student-app. Los CTAs de la landing apuntan aquí. */
  APP_URL: process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000",
  /** URL base del backend API */
  API_URL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
} as const
