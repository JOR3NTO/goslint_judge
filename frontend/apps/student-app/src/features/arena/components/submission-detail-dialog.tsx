"use client"

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/primitives/dialog"
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
      <DialogContent className="max-w-3xl border-border bg-card">
        {submission && (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                Envío a {problemLabel}
                <SubmissionStateBadge submission={submission} />
              </DialogTitle>
              <DialogDescription>{formatDateTimeFull(submission.submittedAt)}</DialogDescription>
            </DialogHeader>

            <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              <Metric label="Lenguaje" value={LANGUAGE_LABELS[submission.language]} />
              <Metric label="Tiempo" value={formatMs(submission.executionTimeMs)} />
              <Metric label="Memoria" value={formatKb(submission.memoryUsedKb)} />
              <Metric label="Tamaño del código" value={`${submission.codeSizeBytes} bytes`} />
            </dl>

            <div className="space-y-2">
              <h3 className="text-sm font-semibold text-foreground">Código fuente</h3>
              <pre className="max-h-[50vh] overflow-auto rounded-md border border-border bg-background p-3 font-mono text-sm leading-6 text-foreground">
                {submission.sourceCode}
              </pre>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-background/40 px-3 py-2">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="font-mono text-foreground">{value}</dd>
    </div>
  )
}
