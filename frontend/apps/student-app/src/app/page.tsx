import { redirect } from "next/navigation"

/**
 * Ruta raíz de student-app.
 * La landing pública vive en landing-app (localhost:3002).
 * Aquí redirigimos a /login directamente.
 */
export default function RootPage() {
  redirect("/login")
}
