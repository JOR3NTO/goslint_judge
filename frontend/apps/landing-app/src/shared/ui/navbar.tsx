"use client"

import { useState } from "react"
import { Button } from "@/shared/ui/primitives/button"
import { env } from "@/core/config/env"
import { Code2, LogIn, User, Menu, X } from "lucide-react"

/** Enlaces ancla de la landing. Los ids existen en las secciones. */
const NAV_LINKS = [
  { href: "#funciones", label: "Funciones" },
  { href: "#maratones", label: "Maratones" },
]

const linkClass =
  "rounded-md px-2 py-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"

/**
 * Navbar pública de landing-app.
 * Enlaces ancla a las secciones de la landing y acceso a student-app (login/registro).
 */
export function Navbar() {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <nav className="fixed inset-x-0 top-0 z-50 border-b border-border bg-background/80 backdrop-blur-xl">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between gap-4">
          <a href="/" className="group flex items-center gap-2 rounded-md focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-primary/30 bg-primary/10 transition-all group-hover:glow-green-sm">
              <Code2 className="h-5 w-5 text-primary" aria-hidden="true" />
            </div>
            <span className="text-xl font-bold text-foreground">
              Goslint<span className="text-primary">Judge</span>
            </span>
          </a>

          {/* Escritorio */}
          <div className="hidden items-center gap-2 md:flex">
            {NAV_LINKS.map((link) => (
              <a key={link.href} href={link.href} className={linkClass}>
                {link.label}
              </a>
            ))}
            <Button asChild variant="ghost" className="ml-2 gap-2 text-muted-foreground hover:text-foreground">
              <a href={`${env.APP_URL}/login`}>
                <LogIn className="h-4 w-4" aria-hidden="true" />
                Iniciar sesión
              </a>
            </Button>
            <Button asChild className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm">
              <a href={`${env.APP_URL}/register`}>
                <User className="h-4 w-4" aria-hidden="true" />
                Registrarse
              </a>
            </Button>
          </div>

          {/* Botón del menú móvil */}
          <button
            type="button"
            onClick={() => setIsOpen((open) => !open)}
            className="rounded-lg p-2 text-muted-foreground hover:bg-secondary hover:text-foreground md:hidden focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
            aria-label={isOpen ? "Cerrar menú" : "Abrir menú"}
            aria-expanded={isOpen}
            aria-controls="menu-movil"
          >
            {isOpen ? <X className="h-6 w-6" aria-hidden="true" /> : <Menu className="h-6 w-6" aria-hidden="true" />}
          </button>
        </div>
      </div>

      {/* Menú móvil */}
      {isOpen && (
        <div id="menu-movil" className="border-t border-border bg-background/95 backdrop-blur-xl md:hidden">
          <div className="space-y-2 px-4 py-4">
            {NAV_LINKS.map((link) => (
              <a
                key={link.href}
                href={link.href}
                onClick={() => setIsOpen(false)}
                className="block rounded-md px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-secondary hover:text-foreground"
              >
                {link.label}
              </a>
            ))}
            <Button asChild variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
              <a href={`${env.APP_URL}/login`} onClick={() => setIsOpen(false)}>
                <LogIn className="h-4 w-4" aria-hidden="true" />
                Iniciar sesión
              </a>
            </Button>
            <Button asChild className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
              <a href={`${env.APP_URL}/register`} onClick={() => setIsOpen(false)}>
                <User className="h-4 w-4" aria-hidden="true" />
                Registrarse
              </a>
            </Button>
          </div>
        </div>
      )}
    </nav>
  )
}
