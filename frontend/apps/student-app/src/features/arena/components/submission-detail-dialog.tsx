"use client"

import { useEffect, useRef, useState } from "react"
import { Check, Copy } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/primitives/dialog"
import { Button } from "@/shared/ui/primitives/button"
import type { SubmissionDTO } from "../types/arena.types"
import { LANGUAGE_LABELS } from "../utils/language"
import { formatDateTimeFull, formatKb, formatMs } from "../utils/format"
import { SubmissionStateBadge } from "./verdict-badge"

interface SubmissionDetailDialogProps {
  submission: SubmissionDTO | null
  problemLabel: string
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** Detalle de un envío: métricas y el código fuente enviado (solo lectura). */
export function SubmissionDetailDialog({
  submission,
  problemLabel,
  open,
  onOpenChange,
}: SubmissionDetailDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl border-border bg-card">
        {submission && (
          <>
            <DialogHeader>
              <DialogTitle className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 pr-8 break-words">
                <span className="min-w-0 break-words">Envío a {problemLabel}</span>
                <SubmissionStateBadge submission={submission} />
              </DialogTitle>
              <DialogDescription className="break-words">
                {formatDateTimeFull(submission.submittedAt)}
              </DialogDescription>
            </DialogHeader>

            <dl className="grid min-w-0 grid-cols-2 gap-3 text-sm lg:grid-cols-4">
              <Metric label="Lenguaje" value={LANGUAGE_LABELS[submission.language]} />
              <Metric label="Tiempo" value={formatMs(submission.executionTimeMs)} />
              <Metric label="Memoria" value={formatKb(submission.memoryUsedKb)} />
              <Metric label="Tamaño del código" value={`${submission.codeSizeBytes} bytes`} />
            </dl>

            <div className="flex w-full min-w-0 flex-col gap-2">
              <div className="flex min-w-0 items-center justify-between gap-2">
                <h3 className="min-w-0 text-sm font-semibold text-foreground">Código fuente</h3>
                <CopyCodeButton key={submission.id} code={submission.sourceCode} />
              </div>
              <pre className="block w-full max-w-full min-w-0 max-h-[50vh] overflow-auto whitespace-pre rounded-md border border-border bg-muted p-3 font-mono text-sm leading-6 text-foreground">
                {submission.sourceCode}
              </pre>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

type CopyStatus = "idle" | "copied" | "failed"

/** Botón para copiar el código. El estado vuelve a "idle" tras 1,5 s. */
function CopyCodeButton({ code }: { code: string }) {
  const [status, setStatus] = useState<CopyStatus>("idle")
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  }, [])

  const flash = (next: CopyStatus) => {
    setStatus(next)
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => setStatus("idle"), 1500)
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code)
      flash("copied")
    } catch {
      flash("failed")
    }
  }

  const copied = status === "copied"
  const label = copied ? "Copiado" : status === "failed" ? "No se pudo copiar" : "Copiar código"

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={handleCopy}
      className="shrink-0 border-border"
    >
      {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
      {label}
    </Button>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-md border border-border bg-muted/40 px-3 py-2">
      <dt className="break-words text-xs text-muted-foreground">{label}</dt>
      <dd className="break-words font-mono text-foreground">{value}</dd>
    </div>
  )
}
