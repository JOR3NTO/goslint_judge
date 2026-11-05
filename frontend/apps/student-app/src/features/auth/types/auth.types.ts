/**
 * Tipos TypeScript para la feature de autenticación.
 *
 * Mapean directamente los DTOs del backend `auth-service` (puerto 8081).
 * Referencia: backend/services/auth-service/docs/AUTH_API.md
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ NOTA: Hoy el backend solo acepta `email` en el login.                 │
 * │ El prototipo .dc.html sugiere "Correo o handle", lo cual requeriría   │
 * │ un cambio en LoginRequest.java → aceptar un campo `identifier` y      │
 * │ buscar tanto por email como por username en LoginUserUseCaseImpl.java. │
 * │ Se deja documentado para implementar en el backend cuando se decida.  │
 * └─────────────────────────────────────────────────────────────────────────┘
 */

// ─── Login ───────────────────────────────────────────────────────────────────

/**
 * Request body para `POST /api/v1/auth/login`.
 * Mapea → `LoginRequest.java` (co.uceva.auth.infrastructure.web.dto)
 *
 * Validaciones del backend:
 * - `email`: @NotBlank + @Email
 * - `password`: @NotBlank
 */
export interface LoginRequest {
    email: string
    password: string
}

/**
 * Response body exitosa de `POST /api/v1/auth/login` (HTTP 200).
 * Mapea → `LoginResponse.java` (co.uceva.auth.infrastructure.web.dto)
 */
export interface LoginResponse {
    /** Token JWT de corta duración (15 min por defecto). */
    accessToken: string
    /** Token JWT de larga duración (8 horas). Guardado en Redis en el backend. */
    refreshToken: string
    /** Tipo de token. Siempre "Bearer". */
    type: string
    /** Tiempo de expiración del access token en milisegundos (900000 = 15 min). */
    expiresIn: number
}

// ─── Errores ─────────────────────────────────────────────────────────────────

/**
 * Mapa de errores de validación del backend (HTTP 400).
 * El backend devuelve un JSON con { campo: mensajeDeError }.
 * Ejemplo: { "email": "El correo electrónico debe ser válido" }
 *
 * Mapea → MethodArgumentNotValidException handler en AuthExceptionHandler.java
 */
export type ValidationErrors = Record<string, string>

/**
 * Respuesta de error genérica del backend.
 * Usada por:
 * - HTTP 401 (BadCredentialsException): { "error": "Credenciales incorrectas" }
 * - HTTP 423 (AccountLockedException): { "error": "Su cuenta ha sido bloqueada por 15 minutos..." }
 * - HTTP 409 (UserAlreadyExistsException): { "error": "El correo ya está registrado." }
 *
 * Mapea → AuthExceptionHandler.java
 */
export interface AuthErrorResponse {
    error: string
}
