"use client"

import { FlaskConical, RotateCcw } from "lucide-react"
import { env } from "@/core/config/env"
import { resetMockData } from "@/core/api/mock"
import { cn } from "@/shared/utils/cn"

/**
 * Chip compacto "Demo" con botón de icono para reiniciar los datos simulados.
 * No renderiza nada salvo que `env.MOCK_ENABLED` sea true (mock activo y no producción).
 * En móvil el botón de reinicio ocupa 44px (zona táctil); en escritorio, 24px.
 */
export function DemoModeBadge({ className }: { className?: string }) {
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
    <div
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border border-warning/30 bg-warning/10 pl-2.5 pr-0.5 text-xs text-warning",
        className
      )}
      title="Modo demo: los datos son simulados"
    >
      <FlaskConical className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
      <span className="font-semibold">Demo</span>
      <button
        type="button"
        onClick={handleReset}
        title="Reiniciar datos demo"
        aria-label="Reiniciar datos demo"
        className="inline-flex h-11 w-11 items-center justify-center rounded-full text-warning transition-colors hover:bg-warning/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-warning md:h-6 md:w-6"
      >
        <RotateCcw className="h-3 w-3" aria-hidden="true" />
      </button>
    </div>
  )
}
