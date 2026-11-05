import type { ProfileResult, UserRole } from "../types/profile.types"

export const ROLE_LABELS: Record<UserRole, string> = {
  STUDENT: "Estudiante",
  ADMIN: "Administrador",
}

type PartialProfile = ProfileResult["profile"]

/** Nombre completo, o el username / parte local del correo si no hay nombre. */
export function getDisplayName(profile: PartialProfile): string {
  const full = [profile.firstName, profile.lastName].filter(Boolean).join(" ").trim()
  return full || profile.username || profile.email.split("@")[0]
}

/** Hasta dos letras en mayúscula para el avatar. */
export function getInitials(profile: PartialProfile): string {
  if (profile.firstName && profile.lastName) {
    return (profile.firstName[0] + profile.lastName[0]).toUpperCase()
  }
  return getDisplayName(profile).slice(0, 2).toUpperCase()
}

/** Fecha larga en español, ej. "1 de septiembre de 2026". */
export function formatDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat("es-CO", { dateStyle: "long" }).format(date)
}
