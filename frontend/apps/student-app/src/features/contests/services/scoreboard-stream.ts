import { env } from "@/core/config/env"
import type { TeamDTO } from "../types/contest.types"

export interface ScoreboardStreamHandlers {
  onOpen?: () => void
  onError?: () => void
  onFrozen?: (frozen: boolean) => void
  onScoreboard: (teams: TeamDTO[]) => void
  onTeamUpdate: (position: number, team: TeamDTO) => void
}

/**
 * Suscribe un EventSource al stream SSE del scoreboard de una maraton.
 * Devuelve una funcion para cerrar la suscripcion.
 */
export function subscribeScoreboardStream(
  contestId: string,
  handlers: ScoreboardStreamHandlers
): () => void {
  if (env.MOCK_ENABLED) return () => {}

  const source = new EventSource(`${env.API_BASE_URL}/v1/contests/${contestId}/scoreboard/stream`)
  source.onopen = () => handlers.onOpen?.()
  source.onerror = () => handlers.onError?.()
  source.addEventListener("frozen", (e) => {
    handlers.onFrozen?.(JSON.parse((e as MessageEvent).data) === true)
  })
  source.addEventListener("scoreboard", (e) => {
    handlers.onScoreboard(JSON.parse((e as MessageEvent).data) as TeamDTO[])
  })
  source.addEventListener("team", (e) => {
    const { position, team } = JSON.parse((e as MessageEvent).data) as { position: number; team: TeamDTO }
    handlers.onTeamUpdate(position, team)
  })

  return () => source.close()
}

/** Inserta o mueve un equipo a su nueva posicion (1-indexed) en la tabla. */
export function applyTeamPositionUpdate(current: TeamDTO[], position: number, team: TeamDTO): TeamDTO[] {
  const rest = current.filter((t) => t.id !== team.id)
  return [...rest.slice(0, position - 1), team, ...rest.slice(position - 1)]
}
