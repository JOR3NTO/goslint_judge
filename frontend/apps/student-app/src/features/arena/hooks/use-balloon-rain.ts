"use client"

import { useEffect, useRef, useState } from "react"
import type { ArenaProblem, SubmissionDTO } from "../types/arena.types"

/** Un globo de la lluvia: posición, tamaño y tiempos repartidos al azar. */
export interface RainBalloon {
  id: number
  /** Posición horizontal en % del ancho de la pantalla. */
  x: number
  /** Ancho en px (el alto lo da la proporción del globo). */
  size: number
  /** Retraso inicial en s. */
  delay: number
  /** Duración del ascenso en s. */
  duration: number
  /** Desplazamiento horizontal total mientras sube, en px. */
  drift: number
  /** Duración de cada vaivén lateral en s. */
  sway: number
}

/** Lluvia de globos de un problema recién aceptado. */
export interface RainBurst {
  key: number
  color: string
  label: string
  balloons: RainBalloon[]
}

const BALLOONS_PER_BURST = 18
/** Tiempo tras el que se retira la ráfaga: retraso máximo + duración máxima + margen. */
const BURST_LIFETIME_MS = 9500

function randomBetween(min: number, max: number): number {
  return min + Math.random() * (max - min)
}

function createBalloons(): RainBalloon[] {
  return Array.from({ length: BALLOONS_PER_BURST }, (_, id) => ({
    id,
    x: randomBetween(3, 94),
    size: Math.round(randomBetween(46, 96)),
    delay: randomBetween(0, 1.6),
    duration: randomBetween(4.6, 7),
    drift: Math.round(randomBetween(-60, 60)),
    sway: randomBetween(1.8, 3),
  }))
}

/**
 * Lluvia de globos a pantalla completa cuando un envío es ACEPTADO en esta visita a la arena.
 *
 * Solo cuenta un envío que no estaba en el historial cargado al abrir la arena y que pasa a ACCEPTED
 * (porque el equipo lo acaba de enviar y el juez lo califica, o porque aparece ya aceptado). Así, entrar a la
 * maratón con envíos aceptados de antes no lanza nada. Cada aceptado genera una ráfaga del color del globo
 * del problema (`ArenaProblem.balloonColor`) que se retira sola al terminar.
 *
 * @param loaded `true` cuando el historial de envíos y el catálogo de problemas ya cargaron: ese momento fija la línea base.
 */
export function useBalloonRain(
  submissions: readonly SubmissionDTO[],
  problems: readonly ArenaProblem[],
  loaded: boolean
): readonly RainBurst[] {
  /** Ids que ya estaban al cargar el historial: nunca disparan. */
  const history = useRef<Set<string> | null>(null)
  /** Ids ya celebrados, para no repetir si la lista se actualiza de nuevo. */
  const celebrated = useRef<Set<string>>(new Set())
  const counter = useRef(0)
  const timers = useRef<Set<ReturnType<typeof setTimeout>>>(new Set())
  const [bursts, setBursts] = useState<readonly RainBurst[]>([])

  useEffect(() => {
    if (!loaded || problems.length === 0) return
    if (!history.current) {
      history.current = new Set(submissions.map((s) => s.id))
      return
    }
    const baseline = history.current

    const created: RainBurst[] = []
    for (const s of submissions) {
      if (s.verdict !== "ACCEPTED" || baseline.has(s.id) || celebrated.current.has(s.id)) continue
      celebrated.current.add(s.id)
      const problem = problems.find((p) => p.id === s.problemId)
      if (!problem) continue
      counter.current += 1
      created.push({
        key: counter.current,
        color: problem.balloonColor,
        label: `¡Problema ${problem.letter} aceptado!`,
        balloons: createBalloons(),
      })
    }
    if (created.length === 0) return

    setBursts((current) => [...current, ...created])
    const timer = setTimeout(() => {
      timers.current.delete(timer)
      const keys = new Set(created.map((b) => b.key))
      setBursts((current) => current.filter((b) => !keys.has(b.key)))
    }, BURST_LIFETIME_MS)
    timers.current.add(timer)
  }, [submissions, problems, loaded])

  useEffect(() => {
    const pending = timers.current
    return () => {
      pending.forEach(clearTimeout)
      pending.clear()
    }
  }, [])

  return bursts
}
