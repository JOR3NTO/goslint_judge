import { redirect } from "next/navigation"
import { env } from "@/core/config/env"

/**
 * Ruta raíz de student-app.
 * El inicio como tal es la landing pública (landing-app), por eso redirigimos
 * allí en lugar de a /login. La URL se configura con NEXT_PUBLIC_LANDING_URL.
 */
export default function RootPage() {
  redirect(env.LANDING_URL)
}
