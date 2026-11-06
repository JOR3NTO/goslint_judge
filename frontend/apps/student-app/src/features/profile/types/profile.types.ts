/**
 * Tipos TypeScript de la feature `profile`.
 *
 * Mapean la tabla `users` de `auth-service`
 * (backend/services/auth-service/.../db/migration/V1__Create_users_table.sql).
 * Nunca incluyen `password_hash`.
 */

/** Roles definidos en `Role.java` del auth-service. */
export type UserRole = "STUDENT" | "ADMIN"

/**
 * Respuesta esperada de `GET /api/v1/auth/me`.
 *
 * Lo expone auth-service (`UserProfileResponse`). Si el endpoint falla con 404/405
 * o no hay conexión, la feature degrada a los claims del JWT
 * (ver `ProfileService.getMyProfile`), sin `institution` ni nombres.
 * `createdAt` en ISO-8601.
 */
export interface UserProfile {
  id: string
  firstName: string
  lastName: string
  username: string
  email: string
  role: UserRole
  /** Institución educativa (opcional en el registro). */
  institution: string | null
  isActive: boolean
  /** Fecha de creación de la cuenta, ISO-8601. */
  createdAt: string
}

/**
 * Claims que emite `JwtTokenProviderAdapter.java`:
 * `sub` = id del usuario, `email`, `role`, `iss`, `iat`, `exp`.
 */
export interface JwtClaims {
  sub: string
  email: string
  role: UserRole
  exp: number
}

/**
 * Perfil resultante de la feature.
 * `isComplete` es `false` cuando solo se pudo construir a partir del JWT
 * (el backend no devolvió el perfil completo).
 */
export interface ProfileResult {
  profile: Partial<UserProfile> & Pick<UserProfile, "id" | "email" | "role">
  isComplete: boolean
}
