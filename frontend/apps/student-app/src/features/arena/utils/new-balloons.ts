import type { TeamDTO } from "@/features/contests"

/** Problemas resueltos por equipo (`teamId` → ids de problema). */
export type SolvedSnapshot = Map<string, Set<string>>

export function snapshotSolved(teams: TeamDTO[]): SolvedSnapshot {
  return new Map(teams.map((team) => [team.id, new Set(team.solvedProblemIds ?? [])]))
}

/**
 * Globos añadidos respecto al snapshot anterior (`teamId` → ids nuevos, en el orden del backend).
 * Un equipo que no estaba en el snapshot no genera globos nuevos: aparece ya con su historial
 * (por ejemplo, al reconectar el SSE, que reenvía la tabla completa).
 */
export function diffNewBalloons(previous: SolvedSnapshot, teams: TeamDTO[]): Map<string, string[]> {
  const added = new Map<string, string[]>()
  for (const team of teams) {
    const before = previous.get(team.id)
    if (!before) continue
    const fresh = (team.solvedProblemIds ?? []).filter((id) => !before.has(id))
    if (fresh.length > 0) added.set(team.id, fresh)
  }
  return added
}
