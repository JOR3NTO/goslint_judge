"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { useLogout } from "@/features/auth"
import { usePendingInvitationsCount } from "@/features/contests"
import { env } from "@/core/config/env"
import { useHasSession } from "@/shared/hooks/use-session"
import { DemoModeBadge } from "@/shared/ui/demo-mode-badge"
import { Logo } from "@/shared/ui/logo"
import { Button } from "@/shared/ui/primitives/button"
import { cn } from "@/shared/utils/cn"
import {
  Trophy,
  History,
  Menu,
  X,
  User,
  LogIn,
  LogOut,
  UserCircle,
  Mail,
} from "lucide-react"

const MOBILE_MENU_ID = "menu-movil"

/** Clases de la ruta activa: texto verde de marca sobre fondo primary suave. */
const ACTIVE_CLASS = "bg-primary/10 font-semibold text-primary hover:bg-primary/15 hover:text-primary"

/** Texto del enlace "Invitaciones" con contador de pendientes (el contador se oculta si es 0 o no se pudo cargar). */
function InvitationsLabel({ count }: { count: number | null }) {
  return (
    <>
      Invitaciones
      {count !== null && count > 0 && (
        <span
          className="ml-1 inline-flex min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-xs font-semibold text-primary-foreground"
          aria-label={`${count} pendientes`}
        >
          {count}
        </span>
      )}
    </>
  )
}

export function Navbar() {
  const [isOpen, setIsOpen] = useState(false)
  const hasSession = useHasSession()
  const { logout, isLoggingOut } = useLogout()
  const pathname = usePathname()
  // Cierra el menú móvil al cambiar de ruta (también con el botón atrás del navegador).
  const [menuPath, setMenuPath] = useState(pathname)
  if (menuPath !== pathname) {
    setMenuPath(pathname)
    setIsOpen(false)
  }
  // Best effort: un fallo al pedir invitaciones no afecta a la navbar.
  const pendingInvitations = usePendingInvitationsCount(hasSession === true)

  /** Ruta activa: se marca con aria-current="page" y con el estilo de ACTIVE_CLASS. */
  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`)
  const navLinkClass = (href: string) =>
    cn("gap-2", isActive(href) ? ACTIVE_CLASS : "text-muted-foreground hover:bg-secondary hover:text-foreground")
  const currentPage = (href: string) => (isActive(href) ? "page" : undefined)

  const profileActive = isActive("/profile")

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-border bg-background/80 backdrop-blur-xl">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo: lleva a la landing pública (otra app/origen, por eso <a> y no next/link) */}
          <a href={env.LANDING_URL} className="flex items-center rounded-md transition-opacity hover:opacity-80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <Logo variant="full" priority className="h-8 sm:h-10" />
          </a>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-1">
            <Link href="/contests" aria-current={currentPage("/contests")}>
              <Button variant="ghost" className={navLinkClass("/contests")}>
                <Trophy className="h-4 w-4" />
                Maratones
              </Button>
            </Link>
            <Link href="/past-contests" aria-current={currentPage("/past-contests")}>
              <Button variant="ghost" className={navLinkClass("/past-contests")}>
                <History className="h-4 w-4" />
                Anteriores
              </Button>
            </Link>
            {hasSession === true && (
              <Link href="/invitations" aria-current={currentPage("/invitations")}>
                <Button variant="ghost" className={navLinkClass("/invitations")}>
                  <Mail className="h-4 w-4" />
                  <InvitationsLabel count={pendingInvitations} />
                </Button>
              </Link>
            )}
          </div>

          {/* Auth Buttons */}
          <div className="hidden md:flex items-center gap-2">
            <DemoModeBadge />
            {hasSession === true && (
              <Link href="/profile" aria-current={currentPage("/profile")} title="Mi perfil">
                <Button
                  variant="ghost"
                  className={cn("gap-2 pl-1.5 pr-2.5", profileActive ? ACTIVE_CLASS : "text-foreground")}
                >
                  <span
                    className={cn(
                      "flex h-7 w-7 items-center justify-center rounded-full border",
                      profileActive
                        ? "border-primary bg-primary/20 text-primary"
                        : "border-border bg-secondary text-muted-foreground"
                    )}
                    aria-hidden="true"
                  >
                    <UserCircle className="size-5" />
                  </span>
                  <span className="hidden lg:inline">Mi perfil</span>
                  <span className="sr-only lg:hidden">Mi perfil</span>
                </Button>
              </Link>
            )}
            {hasSession === true && (
              <Button
                variant="ghost"
                onClick={logout}
                disabled={isLoggingOut}
                className="gap-2 text-muted-foreground hover:bg-secondary hover:text-foreground"
              >
                <LogOut className="h-4 w-4" />
                Cerrar sesión
              </Button>
            )}
            {hasSession === false && (
              <>
                <Link href="/login">
                  <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground">
                    <LogIn className="h-4 w-4" />
                    Iniciar Sesión
                  </Button>
                </Link>
                <Link href="/register">
                  <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                    <User className="h-4 w-4" />
                    Registrarse
                  </Button>
                </Link>
              </>
            )}
          </div>

          {/* Mobile Menu Button (zona táctil de 44px) */}
          <button
            type="button"
            onClick={() => setIsOpen(!isOpen)}
            aria-expanded={isOpen}
            aria-controls={MOBILE_MENU_ID}
            aria-label={isOpen ? "Cerrar menú" : "Abrir menú"}
            className="md:hidden flex h-11 w-11 items-center justify-center rounded-lg text-muted-foreground hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {isOpen ? <X className="h-6 w-6" aria-hidden="true" /> : <Menu className="h-6 w-6" aria-hidden="true" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu: mismos enlaces que el escritorio */}
      {isOpen && (
        <div
          id={MOBILE_MENU_ID}
          className="md:hidden max-h-[calc(100vh-4rem)] overflow-y-auto border-t border-border bg-background/95 backdrop-blur-xl"
        >
          <div className="px-4 py-4 space-y-1">
            <DemoModeBadge className="mb-2" />
            <Link href="/contests" aria-current={currentPage("/contests")} onClick={() => setIsOpen(false)} className="block">
              <Button variant="ghost" className={cn("h-11 w-full justify-start", navLinkClass("/contests"))}>
                <Trophy className="h-4 w-4" />
                Maratones
              </Button>
            </Link>
            <Link href="/past-contests" aria-current={currentPage("/past-contests")} onClick={() => setIsOpen(false)} className="block">
              <Button variant="ghost" className={cn("h-11 w-full justify-start", navLinkClass("/past-contests"))}>
                <History className="h-4 w-4" />
                Anteriores
              </Button>
            </Link>
            {hasSession === true && (
              <Link href="/invitations" aria-current={currentPage("/invitations")} onClick={() => setIsOpen(false)} className="block">
                <Button variant="ghost" className={cn("h-11 w-full justify-start", navLinkClass("/invitations"))}>
                  <Mail className="h-4 w-4" />
                  <InvitationsLabel count={pendingInvitations} />
                </Button>
              </Link>
            )}
            <div className="mt-3 space-y-1 border-t border-border pt-3">
              {hasSession === true && (
                <Link href="/profile" aria-current={currentPage("/profile")} onClick={() => setIsOpen(false)} className="block">
                  <Button
                    variant="ghost"
                    className={cn("h-11 w-full justify-start gap-2", profileActive ? ACTIVE_CLASS : "text-foreground")}
                  >
                    <UserCircle className="size-5" />
                    Mi perfil
                  </Button>
                </Link>
              )}
              {hasSession === true && (
                <Button
                  variant="ghost"
                  onClick={logout}
                  disabled={isLoggingOut}
                  className="h-11 w-full justify-start gap-2 text-muted-foreground hover:text-foreground"
                >
                  <LogOut className="h-4 w-4" />
                  Cerrar sesión
                </Button>
              )}
              {hasSession === false && (
                <>
                  <Link href="/login" className="block">
                    <Button variant="ghost" className="h-11 w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                      <LogIn className="h-4 w-4" />
                      Iniciar Sesión
                    </Button>
                  </Link>
                  <Link href="/register" className="block">
                    <Button className="h-11 w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                      <User className="h-4 w-4" />
                      Registrarse
                    </Button>
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </nav>
  )
}
