/**
 * Public API of the profile feature.
 *
 * Regla de importación (frontend/ARCHITECTURE.md §1): otras features solo
 * importan desde este archivo.
 */
export { ProfilePage } from "./components/profile-page"
export { useProfile } from "./hooks/use-profile"
export { ProfileService, ProfileApiError } from "./services/profile-service"
export type { UserProfile, UserRole, ProfileResult } from "./types/profile.types"
