/**
 * AuthGuard Layout — protege todas las rutas del judge.
 *
 * Como el token está en localStorage (no en cookies), el guard
 * debe correr en el cliente. Next.js middleware no tiene acceso
 * a localStorage (corre en el Edge Runtime, sin window).
 *
 * Flujo:
 * 1. El layout monta en el cliente.
 * 2. Lee localStorage["auth_token"].
 * 3. Si no hay token → router.replace("/login").
 * 4. Si hay token → renderiza {children} normalmente.
 *
 * TODO futuro: migrar token a cookie httpOnly para poder usar
 * middleware de Next.js y tener un guard server-side más seguro.
 */
"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"

/** Debe coincidir con AUTH_TOKEN_KEY en use-login.ts */
const AUTH_TOKEN_KEY = "auth_token"

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const router = useRouter()
  const [isChecking, setIsChecking] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    if (!token) {
      router.replace("/login")
    } else {
      setIsChecking(false)
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

  return <>{children}</>
}
