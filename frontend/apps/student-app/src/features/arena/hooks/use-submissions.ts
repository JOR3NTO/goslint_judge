"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { env } from "@/core/config/env"
import { readContestSession } from "@/features/contests"
import { createArenaService } from "../services/arena-service"
import type {
  SubmissionDTO,
  SubmissionLanguage,
  SubmissionStatusUpdatedEvent,
} from "../types/arena.types"
import { isTerminalStatus } from "../utils/verdict"

/** Subprotocolo que el submission-service acepta junto con `bearer.<token>`. */
const WS_SUBPROTOCOL = "goslint-judge"
/** Intervalo del polling de respaldo para envíos no terminados. */
const POLL_INTERVAL_MS = 3000
const WS_MAX_BACKOFF_MS = 30_000

export interface UseSubmissionsReturn {
  /** Envíos del equipo, más reciente primero. */
  submissions: SubmissionDTO[]
  isLoading: boolean
  /** Error al cargar el historial. */
  error: string | null
  reload: () => void
  /**
   * Envía una solución para el equipo. Añade el envío (PENDING) a la lista y lo devuelve.
   * Lanza `ArenaApiError` si el backend lo rechaza.
   */
  submit: (problemId: string, language: SubmissionLanguage, sourceCode: string) => Promise<SubmissionDTO>
}

/** Inserta o reemplaza un envío por id y mantiene el orden más reciente primero. */
function upsert(list: SubmissionDTO[], incoming: SubmissionDTO[]): SubmissionDTO[] {
  const byId = new Map(list.map((s) => [s.id, s]))
  for (const s of incoming) byId.set(s.id, s)
  return sortRecentFirst([...byId.values()])
}

function sortRecentFirst(list: SubmissionDTO[]): SubmissionDTO[] {
  return [...list].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt))
}

/**
 * Historial de envíos de un equipo con actualización en tiempo real.
 *
 * - WebSocket directo a submission-service: `new WebSocket(url, ["goslint-judge", "bearer.<token>"])`.
 *   Al llegar `SUBMISSION_STATUS_UPDATED` se actualiza el envío local; si no lo conocemos,
 *   se vuelve a pedir el historial. Reconexión con backoff exponencial y limpieza al desmontar.
 * - Respaldo: mientras haya envíos no terminados, polling cada 3 s a `GET /v1/submissions/{id}`
 *   (se detiene al llegar a JUDGED/ENQUEUE_ERROR o al desmontar), para que funcione aunque el WS falle.
 */
export function useSubmissions(teamId: string | null): UseSubmissionsReturn {
  const [submissions, setSubmissions] = useState<SubmissionDTO[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const service = useMemo(() => createArenaService(), [])

  // Espejo de la lista para que el handler del WebSocket pueda saber si conoce un envío
  // sin depender de la versión del estado en el momento de crear la conexión.
  const knownIdsRef = useRef<Set<string>>(new Set())
  useEffect(() => {
    knownIdsRef.current = new Set(submissions.map((s) => s.id))
  }, [submissions])

  // Historial completo del equipo.
  useEffect(() => {
    if (!teamId) {
      setSubmissions([])
      return
    }
    let cancelled = false
    setIsLoading(true)
    setError(null)

    service
      .getTeamSubmissions(teamId)
      .then((list) => {
        if (!cancelled) setSubmissions(sortRecentFirst(list))
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "No se pudo cargar el historial.")
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [teamId, attempt, service])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])

  // WebSocket en tiempo real. En modo mock no se abre: el polling de abajo hace la misma función.
  useEffect(() => {
    if (!teamId || env.MOCK_ENABLED) return
    const token = readContestSession()?.token
    if (!token || typeof WebSocket === "undefined") return

    let disposed = false
    let socket: WebSocket | null = null
    let retryTimer: ReturnType<typeof setTimeout> | null = null
    let attemptsSinceOpen = 0
    let hasOpened = false

    const handleMessage = (raw: string) => {
      let event: SubmissionStatusUpdatedEvent
      try {
        event = JSON.parse(raw) as SubmissionStatusUpdatedEvent
      } catch {
        return
      }
      if (event.type !== "SUBMISSION_STATUS_UPDATED" || event.teamId !== teamId) return

      if (!knownIdsRef.current.has(event.submissionId)) {
        // Envío que no tenemos aún (p.ej. creado en otra pestaña): pedimos el historial completo.
        reload()
        return
      }
      setSubmissions((prev) =>
        prev.map((s) =>
          s.id === event.submissionId
            ? {
                ...s,
                status: event.status,
                verdict: event.verdict,
                executionTimeMs: event.executionTimeMs,
                memoryUsedKb: event.memoryUsedKb,
              }
            : s
        )
      )
    }

    const scheduleRetry = () => {
      if (disposed) return
      const delay = Math.min(WS_MAX_BACKOFF_MS, 1000 * 2 ** attemptsSinceOpen)
      attemptsSinceOpen += 1
      retryTimer = setTimeout(connect, delay)
    }

    function connect() {
      if (disposed) return
      try {
        socket = new WebSocket(env.SUBMISSION_WS_URL, [WS_SUBPROTOCOL, `bearer.${token}`])
      } catch {
        scheduleRetry()
        return
      }
      socket.onopen = () => {
        attemptsSinceOpen = 0
        // Tras una reconexión pueden haberse perdido eventos: refrescamos el historial.
        if (hasOpened) reload()
        hasOpened = true
      }
      socket.onmessage = (ev: MessageEvent) => {
        if (typeof ev.data === "string") handleMessage(ev.data)
      }
      socket.onclose = () => {
        socket = null
        scheduleRetry()
      }
    }

    connect()

    return () => {
      disposed = true
      if (retryTimer) clearTimeout(retryTimer)
      if (socket) {
        socket.onclose = null
        socket.close()
        socket = null
      }
    }
  }, [teamId, reload])

  // Polling de respaldo para envíos no terminados.
  const pendingKey = useMemo(
    () =>
      submissions
        .filter((s) => !isTerminalStatus(s.status))
        .map((s) => s.id)
        .join(","),
    [submissions]
  )

  useEffect(() => {
    if (!pendingKey) return
    const ids = pendingKey.split(",")
    let cancelled = false

    const timer = setInterval(async () => {
      const results = await Promise.allSettled(ids.map((id) => service.getSubmission(id)))
      if (cancelled) return
      const updated = results
        .filter((r): r is PromiseFulfilledResult<SubmissionDTO> => r.status === "fulfilled")
        .map((r) => r.value)
      if (updated.length > 0) setSubmissions((prev) => upsert(prev, updated))
    }, POLL_INTERVAL_MS)

    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [pendingKey, service])

  const submit = useCallback(
    async (problemId: string, language: SubmissionLanguage, sourceCode: string) => {
      if (!teamId) throw new Error("No tienes un equipo inscrito en esta maratón.")
      const created = await service.createSubmission({ teamId, problemId, language, sourceCode })
      setSubmissions((prev) => upsert(prev, [created]))
      return created
    },
    [service, teamId]
  )

  return { submissions, isLoading, error, reload, submit }
}
