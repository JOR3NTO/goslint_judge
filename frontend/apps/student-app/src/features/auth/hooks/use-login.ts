/**
 * useLogin — hook que encapsula toda la lógica del formulario de login.
 *
 * Responsabilidades:
 * 1. Estado del formulario (email, password) con onChange handlers.
 * 2. Validación client-side antes de llamar al backend.
 * 3. Llamada al AuthService → backend POST /api/v1/auth/login.
 * 4. Manejo de los 3 tipos de error del backend:
 *    - HTTP 400: errores de validación por campo.
 *    - HTTP 401: credenciales incorrectas (mensaje genérico).
 *    - HTTP 423: cuenta bloqueada (mensaje con tiempo de espera).
 * 5. Almacenamiento del access token en localStorage.
 * 6. Redirección post-login a /contests.
 *
 * Diseño:
 * - El hook crea su propia instancia de AuthService + HttpApiAdapter.
 *   Cuando se implemente un Provider/Context global de API, se reemplazará
 *   la construcción directa por inyección desde contexto.
 * - Si `NEXT_PUBLIC_USE_MOCK=true`, se usaría MockApiAdapter (pendiente).
 *
 * Uso en el componente:
 * ```tsx
 * const { formData, errors, serverError, isLoading, handleChange, handleSubmit } = useLogin()
 * ```
 */
"use client"

import { useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { HttpApiAdapter } from "@/core/api"
import { env } from "@/core/config/env"
import { AuthService, AuthApiError } from "../services/auth-service"

// ─── Estado del hook ─────────────────────────────────────────────────────────

interface LoginFormData {
  email: string
  password: string
}

interface LoginFormErrors {
  email?: string
  password?: string
}

interface UseLoginReturn {
  /** Datos actuales del formulario. */
  formData: LoginFormData
  /** Errores de validación por campo (client-side o server-side). */
  errors: LoginFormErrors
  /** Error global del servidor (credenciales incorrectas, cuenta bloqueada, red). */
  serverError: string | null
  /** Indica si hay una petición en curso al backend. */
  isLoading: boolean
  /** Handler genérico para inputs controlados. Usa el atributo `name` del input. */
  handleChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  /** Handler del submit del formulario. Valida, llama al backend, redirige. */
  handleSubmit: (e: React.FormEvent) => Promise<void>
}

// ─── Constantes ──────────────────────────────────────────────────────────────

/**
 * Clave de localStorage donde se guarda el access token.
 * El HttpApiAdapter (core/api/http-client.ts) ya lee de esta misma clave
 * para adjuntar el header Authorization: Bearer <token> en cada request.
 */
const AUTH_TOKEN_KEY = "auth_token"

/**
 * Clave de localStorage donde se guarda el refresh token.
 * Se usará cuando se implemente el endpoint de refresh en el backend.
 */
const REFRESH_TOKEN_KEY = "refresh_token"

/** Ruta de destino después de un login exitoso. */
const POST_LOGIN_REDIRECT = "/contests"

// ─── Validación client-side ──────────────────────────────────────────────────

/**
 * Regex para validación básica de email.
 * No pretende cubrir el RFC 5322 completo — eso lo valida el backend con @Email.
 * Su propósito es dar feedback instantáneo al usuario antes de enviar.
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateForm(data: LoginFormData): LoginFormErrors {
  const errors: LoginFormErrors = {}

  if (!data.email.trim()) {
    errors.email = "El correo electrónico es obligatorio."
  } else if (!EMAIL_REGEX.test(data.email)) {
    errors.email = "Debes ingresar un correo válido."
  }

  if (!data.password) {
    errors.password = "La contraseña es obligatoria."
  }

  return errors
}

// ─── Hook ────────────────────────────────────────────────────────────────────

export function useLogin(): UseLoginReturn {
  const router = useRouter()

  const [formData, setFormData] = useState<LoginFormData>({
    email: "",
    password: "",
  })
  const [errors, setErrors] = useState<LoginFormErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  // ── onChange handler ──────────────────────────────────────────────────────

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const { name, value } = e.target
      setFormData((prev) => ({ ...prev, [name]: value }))

      // Limpiar el error del campo cuando el usuario empieza a corregir
      if (errors[name as keyof LoginFormErrors]) {
        setErrors((prev) => {
          const next = { ...prev }
          delete next[name as keyof LoginFormErrors]
          return next
        })
      }

      // Limpiar error de servidor al escribir
      if (serverError) setServerError(null)
    },
    [errors, serverError]
  )

  // ── onSubmit handler ─────────────────────────────────────────────────────

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()

      // 1. Validación client-side
      const validationErrors = validateForm(formData)
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors)
        return
      }

      // 2. Llamada al backend
      setIsLoading(true)
      setServerError(null)
      setErrors({})

      try {
        // Construir el adapter y servicio.
        // TODO: Reemplazar por inyección desde un Provider/Context cuando exista.
        const api = new HttpApiAdapter(env.API_BASE_URL)
        const authService = new AuthService(api)

        const response = await authService.login({
          email: formData.email.trim(),
          password: formData.password,
        })

        // 3. Login exitoso → guardar tokens
        localStorage.setItem(AUTH_TOKEN_KEY, response.accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)

        // 4. Redirigir a la página principal
        router.push(POST_LOGIN_REDIRECT)
      } catch (error: unknown) {
        if (error instanceof AuthApiError) {
          switch (error.status) {
            // HTTP 400 — Errores de validación del backend
            case 400:
              if (error.fieldErrors) {
                setErrors(error.fieldErrors as LoginFormErrors)
              } else {
                setServerError(error.serverMessage ?? "Datos inválidos.")
              }
              break

            // HTTP 401 — Credenciales incorrectas
            case 401:
              setServerError(
                error.serverMessage ?? "Correo electrónico o contraseña incorrectos."
              )
              break

            // HTTP 423 — Cuenta bloqueada por intentos fallidos
            case 423:
              setServerError(
                error.serverMessage ??
                  "Tu cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos."
              )
              break

            // Cualquier otro error HTTP del backend
            default:
              setServerError("Ocurrió un error inesperado. Por favor, intenta de nuevo.")
          }
        } else {
          // Error de red, CORS, timeout, etc. — no viene del backend
          setServerError(
            "No se pudo conectar con el servidor. Verifica tu conexión e intenta de nuevo."
          )
        }
      } finally {
        setIsLoading(false)
      }
    },
    [formData, router]
  )

  return {
    formData,
    errors,
    serverError,
    isLoading,
    handleChange,
    handleSubmit,
  }
}
