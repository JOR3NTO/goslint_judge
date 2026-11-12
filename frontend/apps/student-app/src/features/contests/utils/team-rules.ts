/**
 * Reglas de equipo espejadas del contest-service (solo para validar en cliente y
 * deshabilitar controles). El backend es la autoridad: si cambia una regla, cambia allí.
 *
 * - Máximo de integrantes: Team.DEFAULT_MAX_MEMBERS = 3.
 * - Nombre: TeamName.MIN_LENGTH = 3, MAX_LENGTH = 50 (se valida tras hacer trim).
 * - Invitar: solo el capitán, solo antes del inicio (Contest.assertRegistrationOpen usa la hora,
 *   no el estado), y solo si el equipo tiene cupo.
 */
import type { ContestDTO } from "../types/contest.types"

export const TEAM_MAX_MEMBERS = 3
export const TEAM_NAME_MIN_LENGTH = 3
export const TEAM_NAME_MAX_LENGTH = 50

/** Mensaje de error del nombre del equipo, o `null` si es válido. */
export function validateTeamName(raw: string): string | null {
  const name = raw.trim()
  if (!name) return "El nombre del equipo es obligatorio."
  if (name.length < TEAM_NAME_MIN_LENGTH || name.length > TEAM_NAME_MAX_LENGTH) {
    return `El nombre debe tener entre ${TEAM_NAME_MIN_LENGTH} y ${TEAM_NAME_MAX_LENGTH} caracteres.`
  }
  return null
}

/** Primeros 8 caracteres de un UUID: identificador corto para mostrar integrantes sin nombre. */
export function shortId(id: string): string {
  return id.slice(0, 8)
}

/**
 * ¿Se pueden enviar invitaciones / inscribir ahora? Espejo de Contest.assertRegistrationOpen:
 * el concurso aún no ha iniciado (por hora, no por estado) y no ha terminado.
 */
export function isRegistrationOpen(contest: Pick<ContestDTO, "status" | "startTime">, now = Date.now()): boolean {
  return contest.status === "UPCOMING" && Date.parse(contest.startTime) > now
}
