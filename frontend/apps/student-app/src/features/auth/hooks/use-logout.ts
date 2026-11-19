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
import { clearTokens, readAccessToken } from "@/core/auth/session"
import { AuthService } from "../services/auth-service"

/** Ruta de destino tras cerrar sesión. */
const POST_LOGOUT_REDIRECT = "/login"

export function useLogout() {
  const router = useRouter()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  const logout = useCallback(async () => {
    if (isLoggingOut) return
    setIsLoggingOut(true)

    const token = readAccessToken()
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
