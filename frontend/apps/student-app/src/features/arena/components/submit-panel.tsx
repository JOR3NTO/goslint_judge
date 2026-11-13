"use client"

import { useState } from "react"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/primitives/select"
import { AlertCircle, Loader2, Send } from "lucide-react"
import { ArenaApiError } from "../services/arena-service"
import type { SubmissionDTO, SubmissionLanguage } from "../types/arena.types"
import { CodeEditor } from "./code-editor"
import { SubmissionStateBadge } from "./verdict-badge"
import { LANGUAGE_OPTIONS, LANGUAGE_TEMPLATES } from "../utils/language"
import { readDraft, writeDraft } from "../utils/draft"
import { formatRelativeTime } from "../utils/format"

const DEFAULT_LANGUAGE: SubmissionLanguage = "CPP"
const RECENT_LIMIT = 3

export interface SubmitPanelProps {
  /** Problema seleccionado (id del problem-service). */
  problemId: string
  /** Letra del problema para el encabezado. */
  letter: string
  /** Envíos del equipo (todos los problemas); el panel filtra los de este problema. */
  submissions: SubmissionDTO[]
  /** `false` desactiva el envío (maratón no activa o sin inscripción). */
  canSubmit: boolean
  /** Motivo mostrado cuando `canSubmit` es `false`. */
  disabledReason: string | null
  onSubmit: (language: SubmissionLanguage, sourceCode: string) => Promise<SubmissionDTO>
  /** Abre la pestaña "Mis envíos". */
  onViewAll: () => void
}

/** Columna derecha: editor, envío y los últimos envíos del problema. */
export function SubmitPanel({
  problemId,
  letter,
  submissions,
  canSubmit,
  disabledReason,
  onSubmit,
  onViewAll,
}: SubmitPanelProps) {
  const [language, setLanguage] = useState<SubmissionLanguage>(DEFAULT_LANGUAGE)
  // Borradores en memoria por "problema:lenguaje"; localStorage es la copia persistente.
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitInfo, setSubmitInfo] = useState<string | null>(null)

  const draftKey = `${problemId}:${language}`
  const code = drafts[draftKey] ?? readDraft(problemId, language) ?? LANGUAGE_TEMPLATES[language]

  const updateCode = (value: string) => {
    setDrafts((prev) => ({ ...prev, [draftKey]: value }))
    writeDraft(problemId, language, value)
    setSubmitError(null)
    setSubmitInfo(null)
  }

  const recent = submissions.filter((s) => s.problemId === problemId).slice(0, RECENT_LIMIT)
  const isEmpty = code.trim().length === 0
  const blocked = !canSubmit || submitting

  const handleSubmit = async () => {
    if (blocked) return
    if (isEmpty) {
      setSubmitError("El código está vacío. Escribe una solución antes de enviar.")
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    setSubmitInfo(null)
    try {
      await onSubmit(language, code)
      setSubmitInfo("Solución enviada. El veredicto aparecerá en \"Últimos envíos\".")
    } catch (err: unknown) {
      setSubmitError(
        err instanceof ArenaApiError
          ? err.serverMessage ?? err.message
          : "No se pudo enviar la solución. Inténtalo de nuevo."
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className="border-border bg-card">
      <CardHeader className="gap-3">
        <CardTitle className="flex items-center gap-2 text-lg">
          <span className="font-mono text-primary">{letter}</span>
          Enviar código
        </CardTitle>
        <div className="flex flex-wrap items-center gap-3">
          <label htmlFor="arena-language" className="text-sm text-muted-foreground">
            Lenguaje
          </label>
          <Select
            value={language}
            onValueChange={(value) => {
              setLanguage(value as SubmissionLanguage)
              setSubmitError(null)
              setSubmitInfo(null)
            }}
            disabled={submitting}
          >
            <SelectTrigger id="arena-language" className="w-40 border-border bg-background">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {LANGUAGE_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        <CodeEditor
          id="arena-code"
          value={code}
          onChange={updateCode}
          disabled={submitting}
          ariaLabel={`Código fuente del problema ${letter}`}
        />

        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>{code.length} caracteres</span>
          <span>Tab inserta 4 espacios</span>
        </div>

        {!canSubmit && disabledReason && (
          <p className="rounded-md border border-border bg-secondary/50 px-3 py-2 text-sm text-muted-foreground">
            {disabledReason}
          </p>
        )}

        {submitError && (
          <p role="alert" className="flex items-start gap-2 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
            {submitError}
          </p>
        )}

        {submitInfo && !submitError && (
          <p role="status" className="rounded-md border border-primary/30 bg-primary/10 px-3 py-2 text-sm text-foreground">
            {submitInfo}
          </p>
        )}

        <Button
          type="button"
          onClick={handleSubmit}
          disabled={blocked}
          className="w-full gap-2 bg-primary text-primary-foreground shadow-sm hover:bg-primary/90 glow-green-sm"
        >
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
          {submitting ? "Enviando…" : "Enviar solución"}
        </Button>

        <div className="space-y-2 border-t border-border pt-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-foreground">Últimos envíos</h3>
            <button
              type="button"
              onClick={onViewAll}
              className="text-sm text-primary hover:underline"
            >
              Ver todos
            </button>
          </div>

          {recent.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aún no has enviado una solución a este problema.</p>
          ) : (
            <ul className="space-y-2">
              {recent.map((submission) => (
                <li
                  key={submission.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-border bg-muted/40 px-3 py-2 text-sm"
                >
                  <SubmissionStateBadge submission={submission} />
                  <span className="text-muted-foreground">
                    {LANGUAGE_OPTIONS.find((o) => o.value === submission.language)?.label ?? submission.language}
                  </span>
                  <time dateTime={submission.submittedAt} className="text-muted-foreground">
                    {formatRelativeTime(submission.submittedAt)}
                  </time>
                </li>
              ))}
            </ul>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
