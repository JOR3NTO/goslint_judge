import { env } from "@/core/config/env"
import { HttpApiAdapter, MockApiAdapter } from "@/core/api"
import type { IApiAdapter } from "@/core/api"
import type { UserProfile } from "../types/profile.types"

const mockProfile: UserProfile = {
  id: "00000000-0000-0000-0000-000000000001",
  firstName: "Camila",
  lastName: "Restrepo",
  username: "camila_dev",
  email: "camila.restrepo@uceva.edu.co",
  role: "STUDENT",
  institution: "Unidad Central del Valle del Cauca",
  isActive: true,
  createdAt: "2026-09-01T14:30:00",
}

/**
 * Elige la fuente de datos según `NEXT_PUBLIC_USE_MOCK`
 * (frontend/ARCHITECTURE.md §3 — "Intercambiar implementaciones").
 */
export function createProfileApi(): IApiAdapter {
  if (env.IS_MOCK) {
    return new MockApiAdapter().register("GET", "/v1/auth/me", () => ({ data: mockProfile }))
  }
  return new HttpApiAdapter(env.API_BASE_URL)
}
