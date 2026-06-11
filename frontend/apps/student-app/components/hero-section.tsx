"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ArrowRight, Code2, Sparkles, Terminal } from "lucide-react"

export function HeroSection() {
  return (
    <section className="relative min-h-screen flex items-center justify-center pt-16 overflow-hidden">
      {/* Background Effects */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-primary/10 via-background to-background" />
      <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%2300ff88%22%20fill-opacity%3D%220.03%22%3E%3Ccircle%20cx%3D%221%22%20cy%3D%221%22%20r%3D%221%22%2F%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E')] opacity-50" />
      
      {/* Floating Code Elements */}
      <div className="absolute top-1/4 left-10 opacity-20 text-primary font-mono text-sm hidden lg:block animate-pulse">
        {"while(true) { solve(); }"}
      </div>
      <div className="absolute bottom-1/4 right-10 opacity-20 text-primary font-mono text-sm hidden lg:block animate-pulse delay-1000">
        {"if(accepted) celebrate();"}
      </div>
      <div className="absolute top-1/3 right-1/4 opacity-15 text-primary font-mono text-xs hidden lg:block">
        {"// O(n log n)"}
      </div>
      
      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center space-y-8">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/30 text-primary text-sm font-medium">
            <Sparkles className="h-4 w-4" />
            <span>Plataforma de Programación Competitiva con IA</span>
          </div>

          {/* Main Heading */}
          <h1 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight">
            <span className="text-foreground">Desafía tus</span>
            <br />
            <span className="text-primary text-glow">límites de código</span>
          </h1>

          {/* Description */}
          <p className="max-w-2xl mx-auto text-lg sm:text-xl text-muted-foreground leading-relaxed">
            Participa en maratones de programación, resuelve problemas desafiantes y recibe 
            <span className="text-primary font-medium"> retroalimentación con IA </span> 
            para mejorar tus habilidades de algoritmos.
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link href="/register">
              <Button size="lg" className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green text-lg px-8 py-6">
                Comenzar Ahora
                <ArrowRight className="h-5 w-5" />
              </Button>
            </Link>
            <Link href="/contests">
              <Button size="lg" variant="outline" className="gap-2 border-border hover:border-primary hover:bg-primary/10 text-lg px-8 py-6">
                <Terminal className="h-5 w-5" />
                Ver Maratones
              </Button>
            </Link>
          </div>

          {/* Stats Preview */}
          <div className="pt-12 grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto">
            {[
              { value: "10K+", label: "Usuarios Activos" },
              { value: "500+", label: "Problemas" },
              { value: "150+", label: "Maratones" },
              { value: "98%", label: "Satisfacción" },
            ].map((stat, i) => (
              <div key={i} className="text-center p-4 rounded-lg bg-card/50 border border-border hover:border-primary/30 transition-colors">
                <p className="text-2xl sm:text-3xl font-bold text-primary">{stat.value}</p>
                <p className="text-sm text-muted-foreground">{stat.label}</p>
              </div>
            ))}
          </div>

          {/* Terminal Preview */}
          <div className="mt-16 max-w-4xl mx-auto">
            <div className="rounded-xl overflow-hidden border border-border bg-card shadow-2xl glow-green-sm">
              {/* Terminal Header */}
              <div className="flex items-center gap-2 px-4 py-3 bg-secondary/50 border-b border-border">
                <div className="flex gap-1.5">
                  <div className="h-3 w-3 rounded-full bg-red-500/80" />
                  <div className="h-3 w-3 rounded-full bg-yellow-500/80" />
                  <div className="h-3 w-3 rounded-full bg-green-500/80" />
                </div>
                <div className="flex-1 text-center text-sm text-muted-foreground font-mono">
                  goslint-judge.cpp — GoslintJudge
                </div>
              </div>
              {/* Terminal Content */}
              <div className="p-6 font-mono text-sm text-left space-y-2 bg-[oklch(0.08_0.01_240)]">
                <div className="text-muted-foreground">
                  <span className="text-primary">$</span> goslint submit solution.cpp
                </div>
                <div className="text-muted-foreground">
                  Compilando... <span className="text-primary">✓</span>
                </div>
                <div className="text-muted-foreground">
                  Ejecutando casos de prueba...
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Test 1/10:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-muted-foreground text-xs">(0.01s)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Test 2/10:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-muted-foreground text-xs">(0.02s)</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-muted-foreground">Test 3/10:</span>
                  <span className="text-primary">Accepted</span>
                  <span className="text-muted-foreground text-xs">(0.01s)</span>
                </div>
                <div className="pt-2 text-primary font-semibold">
                  ✓ Todos los tests pasaron! Veredicto: ACCEPTED
                </div>
                <div className="text-muted-foreground cursor-blink">
                  <span className="text-primary">$</span> 
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
