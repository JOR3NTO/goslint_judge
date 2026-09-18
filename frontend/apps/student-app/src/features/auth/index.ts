/**
 * Public API of the auth feature.
 *
 * Regla de importación (frontend/ARCHITECTURE.md §1):
 * Otras features solo pueden importar desde este archivo, nunca desde
 * archivos internos como components/, hooks/, services/ o types/.
 */

// ── Componentes de página ────────────────────────────────────────────────────
export { LoginPage } from "./components/login-page"
export { RegisterPage } from "./components/register-page"

// ── Tipos ────────────────────────────────────────────────────────────────────
export type { LoginRequest, LoginResponse, ValidationErrors, AuthErrorResponse } from "./types/auth.types"

// ── Servicio ─────────────────────────────────────────────────────────────────
export { AuthService, AuthApiError } from "./services/auth-service"

// ── Hooks ────────────────────────────────────────────────────────────────────
export { useLogin } from "./hooks/use-login"
