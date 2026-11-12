"use client"

import { useState } from "react"
import { Button } from "@/shared/ui/primitives/button"
import { Badge } from "@/shared/ui/primitives/badge"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { Check, Clock, Copy, Cpu, FileWarning } from "lucide-react"
import type { ProblemDTO } from "@/features/contests"
import type { SampleDTO } from "../types/arena.types"
import { formatMemoryLimit } from "../utils/format"

interface ProblemStatementProps {
  letter: string | null
  problem: ProblemDTO | null
  samples: SampleDTO[]
  isLoading: boolean
  error: string | null
  samplesFailed: boolean
  onRetry: () => void
}

/** Columna central: enunciado completo del problema seleccionado. */
export function ProblemStatement({
  letter,
  problem,
  samples,
  isLoading,
  error,
  samplesFailed,
  onRetry,
}: ProblemStatementProps) {
  if (!letter) {
    return (
      <section className="flex min-h-[300px] items-center justify-center rounded-lg border border-border bg-card p-6 text-center text-muted-foreground">
        Selecciona un problema de la lista para ver su enunciado.
      </section>
    )
  }

  if (isLoading || (!problem && !error)) {
    return (
      <section className="space-y-4 rounded-lg border border-border bg-card p-6">
        <Skeleton className="h-8 w-2/3" />
        <Skeleton className="h-4 w-1/3" />
        <Skeleton className="h-40 w-full" />
      </section>
    )
  }

  if (error || !problem) {
    return (
      <section className="space-y-3 rounded-lg border border-destructive/40 bg-destructive/10 p-6">
        <p className="flex items-center gap-2 text-foreground">
          <FileWarning className="h-5 w-5 text-destructive" />
          No se pudo cargar el enunciado. {error}
        </p>
        <Button variant="outline" onClick={onRetry}>
          Reintentar
        </Button>
      </section>
    )
  }

  return (
    <article className="space-y-6 rounded-lg border border-border bg-card p-6">
      <header className="space-y-3">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md border border-primary/30 bg-primary/10 font-mono text-lg font-bold text-primary">
            {letter}
          </span>
          <h2 className="text-2xl font-bold text-foreground">{problem.title}</h2>
        </div>
        <div className="flex flex-wrap gap-2 text-sm text-muted-foreground">
          <Badge variant="outline" className="gap-1.5 border-border font-normal">
            <Clock className="h-3.5 w-3.5 text-primary" />
            Tiempo límite: {problem.timeLimitMs} ms
          </Badge>
          <Badge variant="outline" className="gap-1.5 border-border font-normal">
            <Cpu className="h-3.5 w-3.5 text-primary" />
            Memoria límite: {formatMemoryLimit(problem.memoryLimitKb)}
          </Badge>
        </div>
      </header>

      <Section title="Enunciado">
        <p className="whitespace-pre-wrap leading-relaxed text-foreground">{problem.statement}</p>
      </Section>

      {problem.inputFormat && (
        <Section title="Formato de entrada">
          <p className="whitespace-pre-wrap leading-relaxed text-foreground">{problem.inputFormat}</p>
        </Section>
      )}

      {problem.outputFormat && (
        <Section title="Formato de salida">
          <p className="whitespace-pre-wrap leading-relaxed text-foreground">{problem.outputFormat}</p>
        </Section>
      )}

      <Section title="Ejemplos">
        {samplesFailed ? (
          <p className="text-muted-foreground">No se pudieron cargar los ejemplos.</p>
        ) : samples.length === 0 ? (
          <p className="text-muted-foreground">Este problema no tiene ejemplos publicados.</p>
        ) : (
          <div className="space-y-4">
            {samples.map((sample, index) => (
              <div key={sample.id} className="space-y-3 rounded-md border border-border bg-muted/40 p-4">
                <p className="text-sm font-medium text-muted-foreground">Ejemplo {index + 1}</p>
                <SampleBlock label="Entrada" text={sample.input} />
                <SampleBlock label="Salida" text={sample.expectedOutput} />
              </div>
            ))}
          </div>
        )}
      </Section>
    </article>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold uppercase tracking-wide text-primary">{title}</h3>
      {children}
    </section>
  )
}

function SampleBlock({ label, text }: { label: string; text: string }) {
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <CopyButton text={text} label={label} />
      </div>
      <pre className="max-h-48 overflow-auto rounded-md border border-border bg-muted p-3 font-mono text-sm text-foreground">
        {text}
      </pre>
    </div>
  )
}

function CopyButton({ text, label }: { text: string; label: string }) {
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Portapapeles no disponible o sin permiso: no hay nada que mostrar.
    }
  }

  return (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      onClick={copy}
      aria-label={`Copiar ${label.toLowerCase()} del ejemplo`}
      className="h-7 gap-1.5 px-2 text-muted-foreground hover:text-foreground"
    >
      {copied ? <Check className="h-3.5 w-3.5 text-primary" /> : <Copy className="h-3.5 w-3.5" />}
      {copied ? "Copiado" : "Copiar"}
    </Button>
  )
}
