"use client"

import type { CSSProperties } from "react"
import type { RainBurst } from "../hooks/use-balloon-rain"
import { Balloon } from "./balloon"

interface BalloonRainProps {
  bursts: readonly RainBurst[]
}

/**
 * Capa a pantalla completa con los globos que suben al conseguir un aceptado. No captura clics y queda
 * por encima de la interfaz; con `prefers-reduced-motion` no se muestra y solo queda el aviso para lectores
 * de pantalla.
 */
export function BalloonRain({ bursts }: BalloonRainProps) {
  return (
    <>
      <div role="status" aria-live="polite" className="sr-only">
        {bursts.map((burst) => (
          <span key={burst.key}>{burst.label}</span>
        ))}
      </div>
      <div className="balloon-rain pointer-events-none fixed inset-0 z-[60] overflow-hidden" aria-hidden="true">
        {bursts.flatMap((burst) =>
          burst.balloons.map((b) => (
            <div
              key={`${burst.key}-${b.id}`}
              className="balloon-rain-item"
              style={
                {
                  left: `${b.x}%`,
                  width: b.size,
                  height: b.size * 1.6,
                  "--rain-delay": `${b.delay}s`,
                  "--rain-duration": `${b.duration}s`,
                  "--rain-drift": `${b.drift}px`,
                  "--rain-sway": `${b.sway}s`,
                } as CSSProperties
              }
            >
              <div className="balloon-rain-sway">
                <Balloon color={burst.color} label={burst.label} className="h-full w-full" />
              </div>
            </div>
          ))
        )}
      </div>
    </>
  )
}
