"use client"

import { useEffect, useState } from "react"
import { env } from "@/core/config/env"
import { Logo } from "@/shared/ui/logo"

export function Footer() {
  // El año se calcula en cliente para evitar diferencias de hidratación entre servidor y navegador.
  const [year, setYear] = useState<number | null>(null)
  useEffect(() => {
    setYear(new Date().getFullYear())
  }, [])

  return (
    <footer className="border-t border-border bg-muted/40">
      <div className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-10 sm:px-6 md:flex-row md:items-start md:justify-between lg:px-8">
        <div className="max-w-sm space-y-3">
          <a href="/" className="flex items-center rounded-md transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
            <Logo variant="full" className="h-9" />
          </a>
          <p className="text-sm text-muted-foreground">
            Juez de programación competitiva para maratones de programación.
          </p>
        </div>

        <nav aria-label="Enlaces del pie de página">
          <ul className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
            <li>
              <a href="#maratones" className="rounded-sm hover:text-primary focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
                Maratones
              </a>
            </li>
            <li>
              <a href={`${env.APP_URL}/login`} className="rounded-sm hover:text-primary focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
                Iniciar sesión
              </a>
            </li>
            <li>
              <a href={`${env.APP_URL}/register`} className="rounded-sm hover:text-primary focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
                Registrarse
              </a>
            </li>
          </ul>
        </nav>
      </div>

      <div className="border-t border-border">
        <p className="mx-auto max-w-7xl px-4 py-4 text-center text-xs text-muted-foreground sm:px-6 lg:px-8">
          {year !== null ? `© ${year} Goslint Judge · Semillero UCEVA` : "Goslint Judge · Semillero UCEVA"}
        </p>
      </div>
    </footer>
  )
}
