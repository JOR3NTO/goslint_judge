"use client"

import { useLayoutEffect, useRef, useState } from "react"
import type { TeamDTO } from "@/features/contests"
import { diffNewBalloons, snapshotSolved, type SolvedSnapshot } from "../utils/new-balloons"

/** Cuánto dura marcado como "nuevo" un globo (cubre la animación más larga). */
export const NEW_BALLOON_MS = 3200

/**
 * Detecta globos conseguidos en vivo: compara los problemas resueltos de cada equipo con el render anterior.
 * La primera carga (`ready`) solo fija la línea base; reconexiones del SSE o recargas con los mismos datos no
 * marcan nada. Devuelve `teamId` → ids de problema nuevos, que se vacían solos tras `NEW_BALLOON_MS`.
 */
export function useNewBalloons(teams: TeamDTO[], ready: boolean): ReadonlyMap<string, readonly string[]> {
  const previous = useRef<SolvedSnapshot | null>(null)
  const timers = useRef<Set<ReturnType<typeof setTimeout>>>(new Set())
  const [fresh, setFresh] = useState<ReadonlyMap<string, readonly string[]>>(new Map())

  useLayoutEffect(() => {
    if (!ready) return
    const before = previous.current
    previous.current = snapshotSolved(teams)
    if (!before) return

    const added = diffNewBalloons(before, teams)
    if (added.size === 0) return

    setFresh((current) => {
      const next = new Map(current)
      for (const [teamId, ids] of added) next.set(teamId, [...new Set([...(next.get(teamId) ?? []), ...ids])])
      return next
    })

    const timer = setTimeout(() => {
      timers.current.delete(timer)
      setFresh((current) => {
        const next = new Map(current)
        for (const [teamId, ids] of added) {
          const left = (next.get(teamId) ?? []).filter((id) => !ids.includes(id))
          if (left.length > 0) next.set(teamId, left)
          else next.delete(teamId)
        }
        return next
      })
    }, NEW_BALLOON_MS)
    timers.current.add(timer)
  }, [teams, ready])

  useLayoutEffect(() => {
    const pending = timers.current
    return () => {
      pending.forEach(clearTimeout)
      pending.clear()
    }
  }, [])

  return fresh
}
