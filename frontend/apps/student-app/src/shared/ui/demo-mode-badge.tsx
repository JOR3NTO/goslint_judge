"use client"

import { FlaskConical, RotateCcw } from "lucide-react"
import { env } from "@/core/config/env"
import { resetMockData } from "@/core/api/mock"

/**
 * Insignia discreta "Modo demo (datos simulados)" con botón para reiniciar los datos.
 * No renderiza nada salvo que `env.MOCK_ENABLED` sea true (mock activo y no producción).
 */
export function DemoModeBadge() {
  if (!env.MOCK_ENABLED) return null

  const handleReset = () => {
    const confirmed = window.confirm(
      "¿Reiniciar los datos demo? Se borrarán los equipos, inscripciones y envíos creados en esta pestaña y tendrás que volver a iniciar sesión."
    )
    if (!confirmed) return
    resetMockData()
    window.location.reload()
  }

  return (
    <div className="flex items-center gap-2 rounded-full border border-primary/40 bg-primary/10 py-1 pl-2.5 pr-1 text-xs text-primary">
      <FlaskConical className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
      <span className="font-medium whitespace-nowrap">Modo demo (datos simulados)</span>
      <button
        type="button"
        onClick={handleReset}
        className="ml-1 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-primary hover:bg-primary/20 transition-colors"
      >
        <RotateCcw className="h-3 w-3" aria-hidden="true" />
        Reiniciar datos demo
      </button>
    </div>
  )
}
