"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useEffect, useState } from "react"
import { env } from "@/core/config/env"
import { Logo } from "@/shared/ui/logo"

/** Enlaces reales de la plataforma (todos requieren sesión, el footer solo vive en rutas protegidas). */
const FOOTER_LINKS = [
  { href: "/contests", label: "Maratones" },
  { href: "/past-contests", label: "Anteriores" },
  { href: "/invitations", label: "Invitaciones" },
  { href: "/profile", label: "Mi perfil" },
] as const

/** Arena de un concurso: /contests/{id}/arena. Ahí el footer se reduce a una línea para no quitar espacio al editor. */
const ARENA_PATH = /^\/contests\/[^/]+\/arena(\/|$)/

const COPYRIGHT_TEXT = "Goslint Judge · Semillero UCEVA"

/**
 * Año actual calculado tras montar el componente: el servidor y el primer render
 * del cliente no coinciden (evita errores de hidratación si cambia el año).
 */
function useCurrentYear(): number | null {
  const [year, setYear] = useState<number | null>(null)

  useEffect(() => {
    setYear(new Date().getFullYear())
  }, [])

  return year
}

export function Footer() {
  const pathname = usePathname()
  const year = useCurrentYear()
  const isCompact = ARENA_PATH.test(pathname ?? "")
  const copyright = `${year !== null ? `© ${year} ` : "© "}${COPYRIGHT_TEXT}`

  if (isCompact) {
    return (
      <footer className="mt-auto border-t border-border bg-muted/40">
        <p className="px-4 py-2 text-center text-xs text-muted-foreground">{copyright}</p>
      </footer>
    )
  }

  return (
    <footer className="mt-auto border-t border-border bg-muted/40">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          {/* Marca: lleva a la landing pública (otra app/origen, por eso <a> y no next/link) */}
          <div className="flex flex-col items-start gap-2">
            <a
              href={env.LANDING_URL}
              className="flex items-center rounded-md transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-label="Goslint Judge: ir a la página principal"
            >
              <Logo variant="full" className="h-8" />
            </a>
            <p className="text-sm text-muted-foreground">
              Juez de programación competitiva para maratones y prácticas.
            </p>
          </div>

          <nav aria-label="Enlaces del pie de página" className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
            {FOOTER_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="inline-flex min-h-11 items-center text-muted-foreground transition-colors hover:text-primary focus-visible:outline-none focus-visible:text-primary md:min-h-0"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>

        <p className="mt-6 border-t border-border pt-4 text-xs text-muted-foreground">{copyright}</p>
      </div>
    </footer>
  )
}
