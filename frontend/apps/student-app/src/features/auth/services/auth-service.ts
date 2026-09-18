/**
 * AuthService — adapter de la feature `auth` para comunicación con el backend.
 *
 * Sigue el patrón Adapter documentado en frontend/ARCHITECTURE.md §3:
 * - Depende únicamente de `IApiAdapter` (interfaz estable de `core/api`).
 * - Mapea las llamadas al adapter a funciones de dominio tipadas.
 * - Las features consumen este servicio, nunca el adapter directamente.
 *
 * Referencia del endpoint:
 *   POST /api/v1/auth/login → backend/services/auth-service/docs/AUTH_API.md
 *
 * Contrato de errores del backend (AuthExceptionHandler.java):
 *   HTTP 400 → { campo: "mensaje" }  (validación de @Valid)
 *   HTTP 401 → { error: "Credenciales incorrectas" }
 *   HTTP 423 → { error: "Su cuenta ha sido bloqueada por 15 minutos..." }
 */
import type { IApiAdapter } from "@/core/api"
import type { LoginRequest, LoginResponse, AuthErrorResponse, ValidationErrors } from "../types/auth.types"

/**
 * Error tipado que lanza AuthService cuando el backend responde con un error.
 * Permite al consumidor (hook/componente) distinguir el tipo de error por status.
 */
export class AuthApiError extends Error {
  /** HTTP status code del backend (400, 401, 423, etc.) */
  readonly status: number
  /** Errores de validación por campo (solo HTTP 400 con @Valid) */
  readonly fieldErrors?: ValidationErrors
  /** Mensaje de error genérico del backend (HTTP 401, 423) */
  readonly serverMessage?: string

  constructor(status: number, fieldErrors?: ValidationErrors, serverMessage?: string) {
    super(serverMessage ?? "Error de autenticación")
    this.name = "AuthApiError"
    this.status = status
    this.fieldErrors = fieldErrors
    this.serverMessage = serverMessage
  }
}

export class AuthService {
  constructor(private api: IApiAdapter) {}

  /**
   * Autentica un usuario contra `POST /api/v1/auth/login`.
   *
   * @param credentials - Email y contraseña del usuario.
   * @returns Los tokens JWT (access + refresh) y metadatos.
   * @throws {AuthApiError} Si el backend responde con error (400, 401, 423).
   *
   * Flujo del backend (LoginUserUseCaseImpl.java):
   * 1. Verifica si la cuenta está bloqueada en Redis → 423 si lo está.
   * 2. Busca el usuario por email en PostgreSQL.
   * 3. Compara la contraseña con BCrypt.
   * 4. Si falla, incrementa contador en Redis. Al 5to intento → bloqueo 15 min.
   * 5. Si tiene éxito, limpia intentos fallidos y genera access + refresh token.
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await this.api.post<LoginResponse | AuthErrorResponse | ValidationErrors>(
      "/v1/auth/login",
      credentials
    )

    if (!response.ok) {
      const { data, status } = response

      // HTTP 400 — Errores de validación (@Valid en LoginRequest.java)
      // El backend devuelve { campo: "mensaje" }, ej: { "email": "El correo es obligatorio" }
      if (status === 400) {
        throw new AuthApiError(status, data as ValidationErrors)
      }

      // HTTP 401 — Credenciales incorrectas (BadCredentialsException)
      // HTTP 423 — Cuenta bloqueada (AccountLockedException)
      // Ambos devuelven { "error": "mensaje" }
      const errorBody = data as AuthErrorResponse
      throw new AuthApiError(status, undefined, errorBody?.error ?? "Error inesperado del servidor")
    }

    return response.data as LoginResponse
  }
}
