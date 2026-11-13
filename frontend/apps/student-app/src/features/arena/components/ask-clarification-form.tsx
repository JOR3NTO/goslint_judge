"use client"

import { useState, type FormEvent } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Button } from "@/shared/ui/primitives/button"
import { Label } from "@/shared/ui/primitives/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/primitives/select"
import { Textarea } from "@/shared/ui/primitives/textarea"
import { AlertCircle, Loader2, Send } from "lucide-react"
import { ArenaApiError } from "../services/arena-service"
import {
  CLARIFICATION_MAX_LENGTH,
  CLARIFICATION_MIN_LENGTH,
} from "../services/clarifications-service"
import type { ArenaProblem } from "../types/arena.types"

interface AskClarificationFormProps {
  problems: ArenaProblem[]
  /** Problema preseleccionado (el que está abierto en la pestaña Problemas). */
  initialProblemId: string | null
  /** Recibe el id de la relación contest-problem (`ArenaProblem.id`) y la pregunta ya recortada. */
  onAsk: (contestProblemId: string, question: string) => Promise<unknown>
}

/**
 * Formulario para que el equipo pregunte sobre un problema. Solo el estudiante pregunta;
 * las respuestas las publica el organizador en la lista de aclaraciones.
 */
export function AskClarificationForm({ problems, initialProblemId, onAsk }: AskClarificationFormProps) {
  const [problemId, setProblemId] = useState<string>(
    initialProblemId ?? problems[0]?.id ?? ""
  )
  const [question, setQuestion] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const trimmedLength = question.trim().length
  const tooLong = trimmedLength > CLARIFICATION_MAX_LENGTH
  const selectedProblem = problems.find((p) => p.id === problemId) ?? null

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (submitting) return
    setError(null)
    setInfo(null)

    if (!selectedProblem) {
      setError("Elige el problema sobre el que quieres preguntar.")
      return
    }
    const text = question.trim()
    if (text.length < CLARIFICATION_MIN_LENGTH) {
      setError(`La pregunta debe tener al menos ${CLARIFICATION_MIN_LENGTH} caracteres.`)
      return
    }
    if (text.length > CLARIFICATION_MAX_LENGTH) {
      setError(`La pregunta no puede superar ${CLARIFICATION_MAX_LENGTH} caracteres.`)
      return
    }

    setSubmitting(true)
    try {
      await onAsk(selectedProblem.id, text)
      setQuestion("")
      setInfo("Aclaración enviada. El organizador la responderá en esta lista.")
    } catch (err: unknown) {
      setError(
        err instanceof ArenaApiError
          ? err.serverMessage ?? err.message
          : "No se pudo enviar la aclaración. Inténtalo de nuevo."
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className="border-border bg-card">
      <CardHeader className="space-y-1.5">
        <CardTitle className="text-lg">Preguntar al organizador</CardTitle>
        <CardDescription>Describe tu duda con claridad. No incluyas tu código.</CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {problems.length === 0 ? (
            <p className="text-sm text-muted-foreground">No hay problemas disponibles para preguntar.</p>
          ) : (
            <>
              <div className="space-y-2">
                <Label htmlFor="clarification-problem">Problema</Label>
                <Select value={problemId} onValueChange={setProblemId} disabled={submitting}>
                  <SelectTrigger id="clarification-problem" className="w-full border-border bg-background">
                    <SelectValue placeholder="Elige un problema" />
                  </SelectTrigger>
                  <SelectContent>
                    {problems.map((problem) => (
                      <SelectItem key={problem.id} value={problem.id}>
                        {problem.letter} · {problem.title ?? `Problema ${problem.alias}`}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="clarification-question">Pregunta</Label>
                <Textarea
                  id="clarification-question"
                  value={question}
                  onChange={(event) => {
                    setQuestion(event.target.value)
                    setError(null)
                    setInfo(null)
                  }}
                  disabled={submitting}
                  placeholder="Por ejemplo: ¿El arreglo puede estar vacío?"
                  rows={5}
                  aria-describedby="clarification-counter"
                  aria-invalid={tooLong || undefined}
                  className="min-h-32 resize-y border-border bg-background"
                />
                <p
                  id="clarification-counter"
                  className={`text-right text-xs ${tooLong ? "text-destructive" : "text-muted-foreground"}`}
                >
                  {trimmedLength}/{CLARIFICATION_MAX_LENGTH} · mínimo {CLARIFICATION_MIN_LENGTH}
                </p>
              </div>

              {error && (
                <p role="alert" className="flex items-start gap-2 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
                  <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" aria-hidden="true" />
                  {error}
                </p>
              )}

              {info && !error && (
                <p role="status" className="rounded-md border border-primary/30 bg-primary/10 px-3 py-2 text-sm text-foreground">
                  {info}
                </p>
              )}

              <Button
                type="submit"
                disabled={submitting}
                className="w-full gap-2 bg-primary text-primary-foreground shadow-sm hover:bg-primary/90"
              >
                {submitting ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Send className="h-4 w-4" aria-hidden="true" />}
                {submitting ? "Enviando…" : "Enviar pregunta"}
              </Button>
            </>
          )}
        </form>
      </CardContent>
    </Card>
  )
}
