/**
 * ProfileService — adapter de la feature `profile` para el backend.
 *
 * Sigue el patrón Adapter (frontend/ARCHITECTURE.md §3): depende solo de
 * `IApiAdapter`, nunca de la implementación concreta.
 *
 * Endpoint: `GET /api/v1/auth/me` (requiere `Authorization: Bearer <token>`,
 * que `HttpApiAdapter` adjunta automáticamente desde localStorage).
 *
 * Degradación: si el endpoint aún no existe en el backend (404/405) o no hay
 * conexión, se construye un perfil parcial con los claims del JWT
 * (id, email y rol) y se marca `isComplete: false`.
 */
import type { IApiAdapter } from "@/core/api"
import type { ProfileResult, UserProfile } from "../types/profile.types"
import { decodeJwt } from "../utils/decode-jwt"

export class ProfileApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = "ProfileApiError"
    this.status = status
  }

  /** `true` si el token es inválido/expirado/revocado y hay que re-autenticar. */
  get isUnauthorized(): boolean {
    return this.status === 401 || this.status === 403
  }
}

export class ProfileService {
  constructor(private api: IApiAdapter) {}

  /**
   * @param token Access token actual, usado solo para el fallback desde claims.
   * @throws {ProfileApiError} 401/403 si el token no es válido; 5xx si el servidor falla.
   */
  async getMyProfile(token: string): Promise<ProfileResult> {
    let response
    try {
      response = await this.api.get<UserProfile>("/v1/auth/me")
    } catch {
      return this.fromToken(token)
    }

    if (response.ok && response.data) {
      return { profile: response.data, isComplete: true }
    }

    if (response.status === 401 || response.status === 403) {
      throw new ProfileApiError(response.status, "Tu sesión expiró. Inicia sesión de nuevo.")
    }

    if (response.status === 404 || response.status === 405) {
      return this.fromToken(token)
    }

    throw new ProfileApiError(
      response.status,
      response.error ?? "No se pudo cargar tu perfil."
    )
  }

  private fromToken(token: string): ProfileResult {
    const claims = decodeJwt(token)
    if (!claims) {
      throw new ProfileApiError(401, "Tu sesión no es válida. Inicia sesión de nuevo.")
    }
    return {
      profile: { id: claims.sub, email: claims.email, role: claims.role },
      isComplete: false,
    }
  }
}
