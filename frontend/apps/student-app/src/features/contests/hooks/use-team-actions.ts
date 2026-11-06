"use client"

import { useCallback, useState } from "react"
import { createContestsService } from "../services/contests-api"
import { ContestApiError } from "../services/contests-service"
import type { InvitationDTO, TeamDTO } from "../types/contest.types"

/** Acción en curso; `null` si no hay ninguna. Sirve para deshabilitar botones mientras se envía. */
export type TeamActionKind = "invite" | "rename" | "remove" | "leave"

interface UseTeamActionsOptions {
  /** Llamado con el equipo actualizado tras renombrar o quitar a un integrante. */
  onTeamChanged: (team: TeamDTO) => void
  /** Llamado cuando el usuario sale del equipo. */
  onTeamLeft: (teamId: string) => void
}

export interface UseTeamActionsReturn {
  pending: TeamActionKind | null
  /** Último error del backend (o de red), ya en español; `null` si no hay. */
  error: string | null
  clearError: () => void
  invite: (teamId: string, userId: string, email: string) => Promise<InvitationDTO>
  rename: (teamId: string, teamName: string) => Promise<void>
  /** Quita a un integrante. Tras el éxito vuelve a pedir el equipo para tener la lista fresca. */
  removeMember: (teamId: string, userId: string) => Promise<void>
  leave: (teamId: string, userId: string) => Promise<void>
}

/** Convierte cualquier error de la operación en un texto mostrable. */
export function describeTeamError(err: unknown): string {
  if (err instanceof ContestApiError) {
    if (err.serverMessage) return err.serverMessage
    const firstFieldMessage = err.fieldErrors ? Object.values(err.fieldErrors)[0] : undefined
    if (firstFieldMessage) return firstFieldMessage
    if (err.status === 403) return "No tienes permiso para realizar esta acción."
    if (err.status === 404) return "El equipo o la invitación ya no existe."
    if (err.status === 409) return "La acción no se puede completar en el estado actual del equipo."
  }
  return err instanceof TypeError
    ? "No hay conexión con el servidor. Inténtalo de nuevo."
    : err instanceof Error && err.message
      ? err.message
      : "No se pudo completar la operación. Inténtalo de nuevo."
}

/**
 * Acciones de gestión de un equipo (capitán o integrante). Mantiene una sola acción
 * en curso y un único mensaje de error. No decide permisos: eso lo hace el componente,
 * y el backend no verifica al capitán en renombrar ni en quitar integrantes.
 */
export function useTeamActions({ onTeamChanged, onTeamLeft }: UseTeamActionsOptions): UseTeamActionsReturn {
  const [pending, setPending] = useState<TeamActionKind | null>(null)
  const [error, setError] = useState<string | null>(null)

  const clearError = useCallback(() => setError(null), [])

  const run = useCallback(async <T,>(kind: TeamActionKind, op: () => Promise<T>): Promise<T> => {
    setPending(kind)
    setError(null)
    try {
      return await op()
    } catch (err: unknown) {
      setError(describeTeamError(err))
      throw err
    } finally {
      setPending(null)
    }
  }, [])

  const invite = useCallback(
    (teamId: string, userId: string, email: string) =>
      run("invite", () => createContestsService().invite(teamId, { userId, email })),
    [run]
  )

  const rename = useCallback(
    (teamId: string, teamName: string) =>
      run("rename", async () => {
        const team = await createContestsService().updateTeam(teamId, teamName)
        onTeamChanged(team)
      }),
    [run, onTeamChanged]
  )

  const removeMember = useCallback(
    (teamId: string, userId: string) =>
      run("remove", async () => {
        const service = createContestsService()
        await service.removeMember(teamId, userId)
        onTeamChanged(await service.getTeam(teamId))
      }),
    [run, onTeamChanged]
  )

  const leave = useCallback(
    (teamId: string, userId: string) =>
      run("leave", async () => {
        await createContestsService().removeMember(teamId, userId)
        onTeamLeft(teamId)
      }),
    [run, onTeamLeft]
  )

  return { pending, error, clearError, invite, rename, removeMember, leave }
}
