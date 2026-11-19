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
 * - El adapter lo entrega `createHttpApi()`: con `NEXT_PUBLIC_USE_MOCK=true` (fuera de
 *   producción) es el servidor mock, y en otro caso el backend real.
 * - `loginAsDemo()` rellena las credenciales del estudiante demo y entra. Solo lo
 *   muestra la pantalla de login cuando `env.MOCK_ENABLED` es true.
 *
 * Uso en el componente:
 * ```tsx
 * const { formData, errors, serverError, isLoading, handleChange, handleSubmit, loginAsDemo } = useLogin()
 * ```
 */
"use client"

import { useState, useCallback, useEffect } from "react"
import { useRouter } from "next/navigation"
import { createHttpApi } from "@/core/api"
import { sanitizeErrorMessage } from "@/core/api/sanitize-error"
import { consumeSessionNotice, storeTokens } from "@/core/auth/session"
import { DEMO_STUDENT_EMAIL, DEMO_STUDENT_PASSWORD } from "@/core/api/mock"
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
  /** Aviso informativo, p. ej. "Tu sesión expiró, vuelve a iniciar sesión". */
  sessionNotice: string | null
  /** Indica si hay una petición en curso al backend. */
  isLoading: boolean
  /** Handler genérico para inputs controlados. Usa el atributo `name` del input. */
  handleChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  /** Handler del submit del formulario. Valida, llama al backend, redirige. */
  handleSubmit: (e: React.FormEvent) => Promise<void>
  /** Solo modo mock: rellena las credenciales del estudiante demo y entra. */
  loginAsDemo: () => Promise<void>
}

// ─── Constantes ──────────────────────────────────────────────────────────────

/** Ruta de destino después de un login exitoso. */
const POST_LOGIN_REDIRECT = "/contests"

// ─── Validación client-side ──────────────────────────────────────────────────

/**
 * Regex para validación básica de email.
 * No pretende cubrir el RFC 5322 completo — eso lo valida el backend con @Email.
 * Su propósito es avisar al usuario de forma instantánea antes de enviar.
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
  const [sessionNotice, setSessionNotice] = useState<string | null>(null)

  // Aviso que deja la app al cerrar una sesión caducada (se lee una sola vez).
  useEffect(() => {
    setSessionNotice(consumeSessionNotice())
  }, [])

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

  // ── Llamada al backend (compartida por el submit y por el acceso demo) ────

  const performLogin = useCallback(
    async (credentials: LoginFormData) => {
      setIsLoading(true)
      setServerError(null)
      setSessionNotice(null)
      setErrors({})

      try {
        // TODO: Reemplazar por inyección desde un Provider/Context cuando exista.
        const authService = new AuthService(createHttpApi())

        const response = await authService.login({
          email: credentials.email.trim(),
          password: credentials.password,
        })

        // Login exitoso → guardar tokens (core/auth/session es el único que los toca)
        storeTokens(response.accessToken, response.refreshToken)

        // Redirigir a la página principal
        router.push(POST_LOGIN_REDIRECT)
      } catch (error: unknown) {
        if (error instanceof AuthApiError) {
          switch (error.status) {
            // HTTP 400 — Errores de validación del backend
            case 400:
              if (error.fieldErrors) {
                // Solo los campos del formulario, con el texto saneado.
                const fe = error.fieldErrors
                const mapped: LoginFormErrors = {}
                if (fe.email) mapped.email = sanitizeErrorMessage(fe.email, "Revisa el correo electrónico.")
                if (fe.password) mapped.password = sanitizeErrorMessage(fe.password, "Revisa la contraseña.")
                setErrors(mapped)
                if (!mapped.email && !mapped.password) setServerError("Datos inválidos.")
              } else {
                setServerError(sanitizeErrorMessage(error.serverMessage, "Datos inválidos."))
              }
              break

            // HTTP 401 — Credenciales incorrectas
            case 401:
              setServerError(
                sanitizeErrorMessage(error.serverMessage, "Correo electrónico o contraseña incorrectos.")
              )
              break

            // HTTP 423 — Cuenta bloqueada por intentos fallidos
            case 423:
              setServerError(
                sanitizeErrorMessage(
                  error.serverMessage,
                  "Tu cuenta ha sido bloqueada temporalmente por múltiples intentos fallidos."
                )
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
    [router]
  )

  // ── onSubmit handler ─────────────────────────────────────────────────────

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault()

      // Validación client-side
      const validationErrors = validateForm(formData)
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors)
        return
      }

      await performLogin(formData)
    },
    [formData, performLogin]
  )

  // ── Acceso demo (solo modo mock) ─────────────────────────────────────────

  const loginAsDemo = useCallback(async () => {
    const demo = { email: DEMO_STUDENT_EMAIL, password: DEMO_STUDENT_PASSWORD }
    setFormData(demo)
    await performLogin(demo)
  }, [performLogin])

  return {
    formData,
    errors,
    serverError,
    sessionNotice,
    isLoading,
    handleChange,
    handleSubmit,
    loginAsDemo,
  }
}
