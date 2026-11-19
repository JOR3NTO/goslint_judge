/**
 * AuthGuard Layout — protege todas las rutas del judge.
 *
 * Como el token está en localStorage (no en cookies), el guard
 * debe correr en el cliente. Next.js middleware no tiene acceso
 * a localStorage (corre en el Edge Runtime, sin window).
 *
 * Flujo:
 * 1. El layout monta en el cliente.
 * 2. Sin token guardado → router.replace("/login").
 * 3. Con token: se mira su `exp` (core/auth/session). Si caducó se intenta renovar con
 *    el refresh token; si no se puede, la sesión se cierra con el aviso "Tu sesión
 *    expiró, vuelve a iniciar sesión" y se va a /login.
 * 4. La misma comprobación se repite al volver a la pestaña (visibilitychange / focus),
 *    para no seguir usando una sesión caducada tras dejar la pestaña en segundo plano.
 *
 * /login no está bajo este layout, así que no hay bucles de redirección.
 *
 * TODO futuro: migrar token a cookie httpOnly para poder usar
 * middleware de Next.js y tener un guard server-side más seguro.
 */
"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { getValidAccessToken, readAccessToken } from "@/core/auth/session"

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const router = useRouter()
  const [isChecking, setIsChecking] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function check(initial: boolean) {
      if (!readAccessToken()) {
        router.replace("/login")
        return
      }
      // Renueva si caducó; si no se puede, getValidAccessToken cierra la sesión y redirige.
      const token = await getValidAccessToken()
      if (!cancelled && initial && token) setIsChecking(false)
    }

    void check(true)

    const onVisible = () => {
      if (document.visibilityState === "visible") void check(false)
    }
    const onFocus = () => void check(false)
    document.addEventListener("visibilitychange", onVisible)
    window.addEventListener("focus", onFocus)
    return () => {
      cancelled = true
      document.removeEventListener("visibilitychange", onVisible)
      window.removeEventListener("focus", onFocus)
    }
  }, [router])

  // Mientras verifica, muestra nada (evita flash de contenido protegido)
  if (isChecking) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
      </div>
    )
  }

  // Columna de altura mínima de pantalla: la raíz de cada página (y su <main>) ocupa el
  // espacio sobrante, así el footer de la página queda pegado al fondo aunque el contenido sea corto.
  return (
    <div className="flex min-h-screen flex-col [&>div]:flex [&>div]:flex-1 [&>div]:flex-col [&_main]:flex-1">
      {children}
    </div>
  )
}
