"use client"

import { Button } from "@/shared/ui/primitives/button"
import { env } from "@/core/config/env"
import { ArrowRight, Sparkles } from "lucide-react"

/** Hechos verificables de la plataforma (fuente: backend). */
const FACTS = [
  { value: "4", label: "lenguajes: C, C++, Java y Python" },
  { value: "1 a 3", label: "integrantes por equipo" },
  { value: "Tiempo real", label: "veredicto de cada envío" },
  { value: "ICPC", label: "reglas de competencia" },
]

export function HeroSection() {
  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden pt-16">
      {/* Fondo decorativo */}
      <div
        className="absolute inset-0 from-primary/10 via-background to-background"
        style={{ backgroundImage: "radial-gradient(ellipse at top, var(--tw-gradient-stops))" }}
        aria-hidden="true"
      />
      <div
        className="absolute inset-0 opacity-50"
        aria-hidden="true"
        style={{
          backgroundImage:
            "url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%230a7e3a%22%20fill-opacity%3D%220.06%22%3E%3Ccircle%20cx%3D%221%22%20cy%3D%221%22%20r%3D%221%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')",
        }}
      />

      <div className="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div className="space-y-8 text-center">
          {/* Insignia */}
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-4 py-2 text-sm font-medium text-primary">
            <Sparkles className="h-4 w-4" aria-hidden="true" />
            <span>Plataforma de Programación Competitiva</span>
          </div>

          {/* Titular (único h1 de la página) */}
          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
            <span className="text-foreground">Desafía tus</span>
            <br />
            <span className="text-primary">límites de código</span>
          </h1>

          <p className="mx-auto max-w-2xl text-lg leading-relaxed text-muted-foreground sm:text-xl">
            Participa en maratones de programación por equipos, envía tus soluciones en C, C++, Java
            o Python y consulta el veredicto y el ranking de cada maratón.
          </p>

          {/* Botones: mismo ancho en móvil y en escritorio */}
          <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Button
              asChild
              size="lg"
              className="glow-green h-12 w-full bg-primary px-8 text-base text-primary-foreground hover:bg-primary/90 sm:w-56"
            >
              <a href={`${env.APP_URL}/register`}>
                Comenzar ahora
                <ArrowRight className="h-5 w-5" aria-hidden="true" />
              </a>
            </Button>
            <Button
              asChild
              size="lg"
              variant="outline"
              className="h-12 w-full border-border px-8 text-base hover:border-primary hover:bg-primary/10 sm:w-56"
            >
              <a href="#maratones">Ver maratones</a>
            </Button>
          </div>

          {/* Hechos verificables */}
          <dl className="mx-auto grid max-w-3xl grid-cols-2 gap-4 pt-12 md:grid-cols-4 md:gap-6">
            {FACTS.map((fact) => (
              <div
                key={fact.value}
                className="rounded-lg border border-border bg-card p-4 text-center transition-colors hover:border-primary/30"
              >
                <dt className="text-xl font-bold text-primary sm:text-2xl">{fact.value}</dt>
                <dd className="mt-1 text-sm text-muted-foreground">{fact.label}</dd>
              </div>
            ))}
          </dl>

          {/* Ventana de terminal: ilustración */}
          <figure className="mx-auto mt-16 max-w-4xl text-left">
            <div className="flex items-center justify-between gap-4 pb-3">
              <span className="text-sm font-medium text-muted-foreground">Ejemplo ilustrativo</span>
            </div>
            <div className="overflow-hidden rounded-xl border border-border bg-card shadow-md glow-green-sm">
              <div className="flex items-center gap-2 border-b border-border bg-secondary/50 px-4 py-3">
                <div className="flex gap-1.5" aria-hidden="true">
                  <div className="h-3 w-3 rounded-full bg-destructive/70" />
                  <div className="h-3 w-3 rounded-full bg-warning/70" />
                  <div className="h-3 w-3 rounded-full bg-success/70" />
                </div>
                <div className="flex-1 text-center font-mono text-sm text-muted-foreground">
                  solution.cpp — Goslint Judge
                </div>
              </div>
              <div className="space-y-2 bg-muted/40 p-6 font-mono text-sm">
                <div className="text-muted-foreground">
                  Lenguaje: <span className="text-foreground">C++</span>
                </div>
                <div className="text-muted-foreground">
                  Compilando... <span className="text-primary">✓</span>
                </div>
                <div className="text-muted-foreground">Ejecutando casos de prueba...</div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Caso 1/3:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-xs text-muted-foreground">(0.01 s)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Caso 2/3:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-xs text-muted-foreground">(0.02 s)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Caso 3/3:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-xs text-muted-foreground">(0.01 s)</span>
                </div>
                <div className="pt-2 font-semibold text-primary">Veredicto: Accepted</div>
              </div>
            </div>
          </figure>
        </div>
      </div>
    </section>
  )
}
