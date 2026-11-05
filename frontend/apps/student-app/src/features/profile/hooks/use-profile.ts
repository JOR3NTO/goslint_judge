"use client"

import { useCallback, useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { AUTH_TOKEN_KEY } from "@/shared/hooks/use-session"
import { createProfileApi } from "../services/profile-api"
import { ProfileApiError, ProfileService } from "../services/profile-service"
import type { ProfileResult } from "../types/profile.types"

interface UseProfileReturn {
  result: ProfileResult | null
  isLoading: boolean
  error: string | null
  reload: () => void
}

/**
 * Carga el perfil del usuario autenticado.
 * Si el token es inválido, limpia la sesión y redirige a /login.
 */
export function useProfile(): UseProfileReturn {
  const router = useRouter()
  const [result, setResult] = useState<ProfileResult | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    const token = localStorage.getItem(AUTH_TOKEN_KEY)

    if (!token) {
      router.replace("/login")
      return
    }

    setIsLoading(true)
    setError(null)

    new ProfileService(createProfileApi())
      .getMyProfile(token)
      .then((data) => {
        if (!cancelled) setResult(data)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ProfileApiError && err.isUnauthorized) {
          localStorage.removeItem(AUTH_TOKEN_KEY)
          localStorage.removeItem("refresh_token")
          router.replace("/login")
          return
        }
        setError(err instanceof Error ? err.message : "No se pudo cargar tu perfil.")
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [router, attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  return { result, isLoading, error, reload }
}
