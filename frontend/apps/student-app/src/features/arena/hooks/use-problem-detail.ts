"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { createContestsService } from "@/features/contests"
import type { ProblemDTO } from "@/features/contests"
import { createArenaService } from "../services/arena-service"
import type { SampleDTO } from "../types/arena.types"

export interface UseProblemDetailReturn {
  problem: ProblemDTO | null
  samples: SampleDTO[]
  /** El enunciado no pudo cargarse. */
  error: string | null
  /** Las muestras no pudieron cargarse (el enunciado sigue visible). */
  samplesFailed: boolean
  isLoading: boolean
  reload: () => void
}

/** Enunciado completo y casos de muestra de un problema. */
export function useProblemDetail(problemId: string | null): UseProblemDetailReturn {
  const [problem, setProblem] = useState<ProblemDTO | null>(null)
  const [samples, setSamples] = useState<SampleDTO[]>([])
  const [error, setError] = useState<string | null>(null)
  const [samplesFailed, setSamplesFailed] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [attempt, setAttempt] = useState(0)

  const services = useMemo(
    () => ({ contests: createContestsService(), arena: createArenaService() }),
    []
  )

  useEffect(() => {
    if (!problemId) return
    let cancelled = false

    setIsLoading(true)
    setError(null)
    setSamplesFailed(false)

    Promise.allSettled([
      services.contests.getProblem(problemId),
      services.arena.getSamples(problemId),
    ]).then(([problemRes, samplesRes]) => {
      if (cancelled) return
      if (problemRes.status === "rejected") {
        setProblem(null)
        setSamples([])
        setError(
          problemRes.reason instanceof Error
            ? problemRes.reason.message
            : "No se pudo cargar el problema."
        )
      } else {
        setProblem(problemRes.value)
        setSamples(
          samplesRes.status === "fulfilled"
            ? [...samplesRes.value].sort((a, b) => a.orderIndex - b.orderIndex)
            : []
        )
        setSamplesFailed(samplesRes.status === "rejected")
      }
      setIsLoading(false)
    })

    return () => {
      cancelled = true
    }
  }, [problemId, attempt, services])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  return { problem, samples, error, samplesFailed, isLoading, reload }
}
