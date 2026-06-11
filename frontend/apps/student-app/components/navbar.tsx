"use client"

import Link from "next/link"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Code2,
  Trophy,
  History,
  Sparkles,
  Menu,
  X,
  User,
  LogIn,
} from "lucide-react"

export function Navbar() {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-border bg-background/80 backdrop-blur-xl">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2 group">
            <div className="relative flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 border border-primary/30 group-hover:glow-green-sm transition-all">
              <Code2 className="h-5 w-5 text-primary" />
            </div>
            <span className="text-xl font-bold text-foreground">
              Goslint<span className="text-primary">Judge</span>
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-1">
            <Link href="/contests">
              <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground hover:bg-secondary">
                <Trophy className="h-4 w-4" />
                Maratones
              </Button>
            </Link>
            <Link href="/past-contests">
              <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground hover:bg-secondary">
                <History className="h-4 w-4" />
                Anteriores
              </Button>
            </Link>
            <Link href="/ai-feedback">
              <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground hover:bg-secondary">
                <Sparkles className="h-4 w-4" />
                AI Feedback
              </Button>
            </Link>
          </div>

          {/* Auth Buttons */}
          <div className="hidden md:flex items-center gap-3">
            <Link href="/login">
              <Button variant="ghost" className="gap-2 text-muted-foreground hover:text-foreground">
                <LogIn className="h-4 w-4" />
                Iniciar Sesión
              </Button>
            </Link>
            <Link href="/register">
              <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm">
                <User className="h-4 w-4" />
                Registrarse
              </Button>
            </Link>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden p-2 rounded-lg text-muted-foreground hover:text-foreground hover:bg-secondary"
          >
            {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {isOpen && (
        <div className="md:hidden border-t border-border bg-background/95 backdrop-blur-xl">
          <div className="px-4 py-4 space-y-2">
            <Link href="/contests" onClick={() => setIsOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                <Trophy className="h-4 w-4" />
                Maratones
              </Button>
            </Link>
            <Link href="/past-contests" onClick={() => setIsOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                <History className="h-4 w-4" />
                Maratones Anteriores
              </Button>
            </Link>
            <Link href="/ai-feedback" onClick={() => setIsOpen(false)}>
              <Button variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                <Sparkles className="h-4 w-4" />
                AI Feedback
              </Button>
            </Link>
            <div className="pt-4 border-t border-border space-y-2">
              <Link href="/login" onClick={() => setIsOpen(false)}>
                <Button variant="ghost" className="w-full justify-start gap-2 text-muted-foreground hover:text-foreground">
                  <LogIn className="h-4 w-4" />
                  Iniciar Sesión
                </Button>
              </Link>
              <Link href="/register" onClick={() => setIsOpen(false)}>
                <Button className="w-full gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                  <User className="h-4 w-4" />
                  Registrarse
                </Button>
              </Link>
            </div>
          </div>
        </div>
      )}
    </nav>
  )
}
