"use client"

import { useCallback, useEffect, useState } from "react"
import { createContestsService } from "../services/contests-api"
import type { ContestDTO, InvitationDTO, TeamDTO } from "../types/contest.types"
import { describeTeamError } from "./use-team-actions"

/** Invitación con el equipo y la maratón a los que pertenece (`null` si no se pudieron cargar). */
export interface InvitationItem {
  invitation: InvitationDTO
  team: TeamDTO | null
  contest: ContestDTO | null
}

interface UseMyInvitationsReturn {
  /** `null` mientras no hay datos todavía. */
  items: InvitationItem[] | null
  isLoading: boolean
  /** Error al cargar la lista (red o backend). */
  error: string | null
  reload: () => void
  /** Acepta; devuelve el equipo. Lanza el error del backend tras guardarlo en `actionErrors`. */
  accept: (invitationId: string) => Promise<TeamDTO>
  reject: (invitationId: string) => Promise<void>
  /** Invitación con una acción en curso, o `null`. */
  busyId: string | null
  /** Error de la última acción por id de invitación. */
  actionErrors: Record<string, string>
}

/** PENDING primero, luego las más recientes. */
function sortItems(items: InvitationItem[]): InvitationItem[] {
  const rank = (status: InvitationDTO["status"]) => (status === "PENDING" ? 0 : 1)
  return [...items].sort((a, b) => {
    const byStatus = rank(a.invitation.status) - rank(b.invitation.status)
    if (byStatus !== 0) return byStatus
    return Date.parse(b.invitation.createdAt) - Date.parse(a.invitation.createdAt)
  })
}

/**
 * Carga `GET /v1/invitations/me` y, para cada invitación, su equipo (`GET /v1/teams/{id}`)
 * y su maratón (`GET /v1/contests/{id}`). Cada equipo y maratón se pide una sola vez.
 * Las cargas de equipo/maratón son best effort: si fallan, la tarjeta igual se muestra.
 */
export function useMyInvitations(): UseMyInvitationsReturn {
  const [items, setItems] = useState<InvitationItem[] | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [actionErrors, setActionErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    let cancelled = false
    const service = createContestsService()
    setIsLoading(true)

    async function load() {
      try {
        const invitations = await service.getMyInvitations()
        const teamIds = [...new Set(invitations.map((i) => i.teamId))]
        const teamResults = await Promise.allSettled(teamIds.map((id) => service.getTeam(id)))
        const teamById = new Map<string, TeamDTO>()
        teamResults.forEach((r, i) => {
          if (r.status === "fulfilled") teamById.set(teamIds[i], r.value)
        })

        const contestIds = [...new Set([...teamById.values()].map((t) => t.contestId))]
        const contestResults = await Promise.allSettled(contestIds.map((id) => service.getById(id)))
        const contestById = new Map<string, ContestDTO>()
        contestResults.forEach((r, i) => {
          if (r.status === "fulfilled") contestById.set(contestIds[i], r.value)
        })

        if (cancelled) return
        const built = invitations.map<InvitationItem>((invitation) => {
          const team = teamById.get(invitation.teamId) ?? null
          return {
            invitation,
            team,
            contest: team ? contestById.get(team.contestId) ?? null : null,
          }
        })
        setItems(sortItems(built))
        setError(null)
      } catch (err: unknown) {
        if (!cancelled) setError(describeTeamError(err))
      } finally {
        if (!cancelled) setIsLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  const patchInvitation = useCallback((invitationId: string, patch: Partial<InvitationDTO>, team?: TeamDTO) => {
    setItems((prev) =>
      prev?.map((item) =>
        item.invitation.id === invitationId
          ? {
              ...item,
              invitation: { ...item.invitation, ...patch },
              team: team ?? item.team,
            }
          : item
      ) ?? prev
    )
  }, [])

  const accept = useCallback(
    async (invitationId: string): Promise<TeamDTO> => {
      setBusyId(invitationId)
      setActionErrors((prev) => omitKey(prev, invitationId))
      try {
        const team = await createContestsService().acceptInvitation(invitationId)
        patchInvitation(invitationId, { status: "ACCEPTED", respondedAt: new Date().toISOString() }, team)
        return team
      } catch (err: unknown) {
        setActionErrors((prev) => ({ ...prev, [invitationId]: describeTeamError(err) }))
        // Puede que el backend haya marcado la invitación como vencida: se recarga para mostrar el estado real.
        reload()
        throw err
      } finally {
        setBusyId(null)
      }
    },
    [patchInvitation, reload]
  )

  const reject = useCallback(
    async (invitationId: string): Promise<void> => {
      setBusyId(invitationId)
      setActionErrors((prev) => omitKey(prev, invitationId))
      try {
        await createContestsService().rejectInvitation(invitationId)
        patchInvitation(invitationId, { status: "REJECTED", respondedAt: new Date().toISOString() })
      } catch (err: unknown) {
        setActionErrors((prev) => ({ ...prev, [invitationId]: describeTeamError(err) }))
        reload()
        throw err
      } finally {
        setBusyId(null)
      }
    },
    [patchInvitation, reload]
  )

  return { items, isLoading, error, reload, accept, reject, busyId, actionErrors }
}

function omitKey(record: Record<string, string>, key: string): Record<string, string> {
  const next = { ...record }
  delete next[key]
  return next
}
