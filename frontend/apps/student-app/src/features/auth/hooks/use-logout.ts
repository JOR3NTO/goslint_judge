/**
 * useLogout — cierra la sesión del usuario.
 *
 * 1. Llama a `POST /api/v1/auth/logout` con el access token (si hay uno).
 * 2. Pase lo que pase con la red o la respuesta, borra `auth_token` y
 *    `refresh_token` de localStorage: la sesión local se cierra siempre.
 * 3. Redirige a /login.
 */
"use client"

import { useCallback, useState } from "react"
import { useRouter } from "next/navigation"
import { createHttpApi } from "@/core/api"
import { AuthService } from "../services/auth-service"

/** Claves de localStorage. Deben coincidir con use-login.ts y use-session.ts. */
const AUTH_TOKEN_KEY = "auth_token"
const REFRESH_TOKEN_KEY = "refresh_token"

/** Ruta de destino tras cerrar sesión. */
const POST_LOGOUT_REDIRECT = "/login"

function readToken(): string | null {
  try {
    return localStorage.getItem(AUTH_TOKEN_KEY)
  } catch {
    return null
  }
}

function clearTokens(): void {
  try {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    // Almacenamiento no disponible: no hay nada que borrar.
  }
}

export function useLogout() {
  const router = useRouter()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const logout = useCallback(async () => {
    if (isLoggingOut) return
    setIsLoggingOut(true)

    const token = readToken()
    try {
      if (token) {
        await new AuthService(createHttpApi()).logout(token)
      }
    } catch {
      // Sin conexión o error del servidor: la sesión local se cierra igual.
    } finally {
      clearTokens()
      router.replace(POST_LOGOUT_REDIRECT)
      setIsLoggingOut(false)
    }
  }, [isLoggingOut, router])

  return { logout, isLoggingOut }
}
