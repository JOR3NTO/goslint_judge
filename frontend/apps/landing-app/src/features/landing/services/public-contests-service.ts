/**
 * PublicContestsService: obtiene las maratones públicas desde el backend.
 *
 * Endpoint: GET {env.API_URL}/api/v1/contests (sin autenticación).
 * Si la petición falla, el error se propaga a la UI: nunca se muestran datos de respaldo.
 */
import { env } from "@/core/config/env"
import type {
  ApiContest,
  ApiContestStatus,
  ContestStatus,
  PublicContest,
} from "../types/public-contest.types"

const STATUS_MAP: Record<ApiContestStatus, ContestStatus> = {
  ACTIVE: "active",
  UPCOMING: "upcoming",
  ENDED: "past",
}

function toPublicContest(raw: ApiContest): PublicContest | null {
  const status = STATUS_MAP[raw.status]
  const startTime = new Date(raw.startTime)
  const endTime = new Date(raw.endTime)
  if (!status || !raw.id || !raw.title) return null
  if (Number.isNaN(startTime.getTime()) || Number.isNaN(endTime.getTime())) return null
  return {
    id: raw.id,
    title: raw.title,
    description: raw.description ?? "",
    status,
    startTime,
    endTime,
  }
}

/**
 * Devuelve todas las maratones públicas, ordenadas por pestaña:
 * activas (la que termina antes primero), próximas (la que inicia antes primero)
 * y pasadas (la más reciente primero).
 */
export async function getPublicContests(signal?: AbortSignal): Promise<PublicContest[]> {
  const res = await fetch(`${env.API_URL}/api/v1/contests`, {
    headers: { Accept: "application/json" },
    cache: "no-store",
    signal,
  })
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }

  const body: unknown = await res.json()
  if (!Array.isArray(body)) {
    throw new Error("Respuesta inesperada del servidor de maratones")
  }

  const contests = (body as ApiContest[])
    .map(toPublicContest)
    .filter((c): c is PublicContest => c !== null)

  return contests.sort((a, b) => {
    const rank: Record<ContestStatus, number> = { active: 0, upcoming: 1, past: 2 }
    if (a.status !== b.status) return rank[a.status] - rank[b.status]
    if (a.status === "active") return a.endTime.getTime() - b.endTime.getTime()
    if (a.status === "upcoming") return a.startTime.getTime() - b.startTime.getTime()
    return b.endTime.getTime() - a.endTime.getTime()
  })
}
