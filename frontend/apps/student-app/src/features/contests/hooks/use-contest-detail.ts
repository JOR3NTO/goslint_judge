"use client"

import { useCallback, useEffect, useState } from "react"
import { ContestApiError } from "../services/contests-service"
import { createContestsService } from "../services/contests-api"
import type {
  ContestDTO,
  ContestProblemItem,
  TeamDTO,
} from "../types/contest.types"
import { readContestSession } from "../utils/session"

/** Secciones que pueden fallar por separado sin tumbar la página. */
export interface ContestSectionErrors {
  problems: boolean
  scoreboard: boolean
  teams: boolean
}

interface UseContestDetailReturn {
  contest: ContestDTO | null
  problems: ContestProblemItem[]
  scoreboard: TeamDTO[]
  teams: TeamDTO[]
  /** Equipo del usuario autenticado en ESTA maratón, o `null`. */
  myTeam: TeamDTO | null
  /** Error al cargar la maratón (sin ella no hay nada que mostrar). */
  error: string | null
  sectionErrors: ContestSectionErrors
  isLoading: boolean
  reload: () => void
  /**
   * Inscribe un equipo en la maratón. Lanza `ContestApiError` con los errores
   * del backend para que el diálogo los muestre.
   */
  joinContest: (teamName: string, contestPassword: string) => Promise<void>
}

const NO_SECTION_ERRORS: ContestSectionErrors = { problems: false, scoreboard: false, teams: false }

function describeError(err: unknown): string {
  if (err instanceof ContestApiError && err.status === 404) {
    return "Esta maratón no existe o ya no está disponible."
  }
  return err instanceof Error ? err.message : "No se pudo cargar la maratón."
}

/**
 * Carga el detalle de una maratón: datos base, problemas (con título cuando
 * el problem-service responde), scoreboard, equipos y si el usuario ya está inscrito.
 */
export function useContestDetail(contestId: string): UseContestDetailReturn {
  const [contest, setContest] = useState<ContestDTO | null>(null)
  const [problems, setProblems] = useState<ContestProblemItem[]>([])
  const [scoreboard, setScoreboard] = useState<TeamDTO[]>([])
  const [teams, setTeams] = useState<TeamDTO[]>([])
  const [myTeam, setMyTeam] = useState<TeamDTO | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [sectionErrors, setSectionErrors] = useState<ContestSectionErrors>(NO_SECTION_ERRORS)
  const [isLoading, setIsLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    const service = createContestsService()
    const session = readContestSession()

    setIsLoading(true)
    setError(null)

    async function load() {
      let base: ContestDTO
      try {
        base = await service.getById(contestId)
      } catch (err: unknown) {
        if (!cancelled) {
          setContest(null)
          setError(describeError(err))
          setIsLoading(false)
        }
        return
      }

      const [problemsRes, scoreboardRes, teamsRes, mineRes] = await Promise.allSettled([
        service.getProblems(contestId),
        service.getScoreboard(contestId),
        service.getTeams(contestId),
        session ? service.getMyTeams(session.userId) : Promise.resolve<TeamDTO[]>([]),
      ])
      if (cancelled) return

      // Título de cada problema: best effort, si falla queda null (se muestra el alias).
      const problemDtos =
        problemsRes.status === "fulfilled"
          ? [...problemsRes.value].sort((a, b) => a.orderIndex - b.orderIndex)
          : []
      const titles = await Promise.allSettled(
        problemDtos.map((p) => service.getProblem(p.problemId))
      )
      if (cancelled) return

      setContest(base)
      setProblems(
        problemDtos.map((p, i) => {
          const title = titles[i]
          return {
            ...p,
            title: title.status === "fulfilled" ? title.value.title : null,
          }
        })
      )
      setScoreboard(scoreboardRes.status === "fulfilled" ? scoreboardRes.value : [])
      setTeams(teamsRes.status === "fulfilled" ? teamsRes.value : [])
      setMyTeam(
        mineRes.status === "fulfilled"
          ? mineRes.value.find((t) => t.contestId === contestId) ?? null
          : null
      )
      setSectionErrors({
        problems: problemsRes.status === "rejected",
        scoreboard: scoreboardRes.status === "rejected",
        teams: teamsRes.status === "rejected",
      })
      setIsLoading(false)
    }

    load()

    return () => {
      cancelled = true
    }
  }, [contestId, attempt])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  const joinContest = useCallback(
    async (teamName: string, contestPassword: string) => {
      if (!readContestSession()) {
        throw new ContestApiError(401, undefined, "Debes iniciar sesión para inscribirte.")
      }

      const service = createContestsService()
      const team = await service.joinContest(contestId, teamName, contestPassword)

      setMyTeam(team)
      setTeams((prev) => [...prev, team])

      // Refresca el ranking sin volver a mostrar el skeleton; si falla, se conserva el anterior.
      service
        .getScoreboard(contestId)
        .then(setScoreboard)
        .catch(() => setSectionErrors((prev) => ({ ...prev, scoreboard: true })))
    },
    [contestId]
  )

  return {
    contest,
    problems,
    scoreboard,
    teams,
    myTeam,
    error,
    sectionErrors,
    isLoading,
    reload,
    joinContest,
  }
}
