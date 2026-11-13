"use client"

import { useCallback, useEffect, useState } from "react"
import {
  createContestsService,
  readContestSession,
} from "@/features/contests"
import type { ContestDTO, ContestProblemDTO, TeamDTO } from "@/features/contests"
import type { ArenaProblem } from "../types/arena.types"
import { problemLetter } from "../utils/format"

export interface UseArenaReturn {
  contest: ContestDTO | null
  /** Problemas ordenados por `orderIndex`, con título y dificultad cuando se pudieron cargar. */
  problems: ArenaProblem[]
  /** Equipo del usuario en ESTA maratón; `null` si no está inscrito. */
  team: TeamDTO | null
  /** Error que impide entrar (sin sesión, maratón inexistente, inscripción no verificable). */
  error: string | null
  /** La lista de problemas no pudo cargarse (el resto de la arena sigue visible). */
  problemsFailed: boolean
  isLoading: boolean
  reload: () => void
}

/**
 * Carga lo necesario para la arena: la maratón, sus problemas (con título y dificultad
 * del problem-service, best effort) y el equipo del usuario en esa maratón.
 */
export function useArena(contestId: string): UseArenaReturn {
  const [contest, setContest] = useState<ContestDTO | null>(null)
  const [problems, setProblems] = useState<ArenaProblem[]>([])
  const [team, setTeam] = useState<TeamDTO | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [problemsFailed, setProblemsFailed] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    const service = createContestsService()
    // Leída en el efecto (solo cliente): localStorage no existe durante el render del servidor.
    const session = readContestSession()

    setIsLoading(true)
    setError(null)

    async function load() {
      if (!session) {
        if (!cancelled) {
          setError("Debes iniciar sesión para entrar a la arena.")
          setIsLoading(false)
        }
        return
      }

      let base: ContestDTO
      try {
        base = await service.getById(contestId)
      } catch (err: unknown) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No se pudo cargar la maratón.")
          setIsLoading(false)
        }
        return
      }

      const [problemsRes, mineRes] = await Promise.allSettled([
        service.getProblems(contestId),
        service.getMyTeams(session.userId),
      ])
      if (cancelled) return

      // Sin la lista de equipos no se puede decidir si el usuario está inscrito: error explícito.
      if (mineRes.status === "rejected") {
        setContest(base)
        setError("No se pudo verificar tu inscripción en esta maratón.")
        setIsLoading(false)
        return
      }

      const sorted: ContestProblemDTO[] =
        problemsRes.status === "fulfilled"
          ? [...problemsRes.value].sort((a, b) => a.orderIndex - b.orderIndex)
          : []

      // Título y dificultad: una petición por problema, cada una puede fallar sin tumbar la vista.
      const details = await Promise.allSettled(sorted.map((p) => service.getProblem(p.problemId)))
      if (cancelled) return

      setContest(base)
      setTeam(mineRes.value.find((t) => t.contestId === contestId) ?? null)
      setProblemsFailed(problemsRes.status === "rejected")
      setProblems(
        sorted.map((p, index) => {
          const detail = details[index]
          const loaded = detail.status === "fulfilled" ? detail.value : null
          return {
            id: p.id,
            problemId: p.problemId,
            letter: problemLetter(index),
            alias: p.alias,
            orderIndex: p.orderIndex,
            points: p.points,
            title: loaded?.title ?? null,
            difficulty: loaded?.difficult ?? null,
          }
        })
      )
      setIsLoading(false)
    }

    load()

    return () => {
      cancelled = true
    }
  }, [contestId, attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  return { contest, problems, team, error, problemsFailed, isLoading, reload }
}
