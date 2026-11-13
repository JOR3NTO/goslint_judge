"use client"

import { useState } from "react"
import { Alert, AlertDescription } from "@/shared/ui/primitives/alert"
import { Button } from "@/shared/ui/primitives/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/primitives/dialog"
import { Input } from "@/shared/ui/primitives/input"
import { Label } from "@/shared/ui/primitives/label"
import { Loader2 } from "lucide-react"
import { ContestApiError } from "../services/contests-service"
import type { ContestFieldErrors } from "../types/contest.types"
import {
  TEAM_MAX_MEMBERS,
  TEAM_NAME_MAX_LENGTH,
  TEAM_NAME_MIN_LENGTH,
  validateTeamName,
} from "../utils/team-rules"

interface JoinContestDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  contestTitle: string
  /** Debe lanzar `ContestApiError` si el backend rechaza la inscripción. */
  onJoin: (teamName: string, contestPassword: string) => Promise<void>
}

const FIELD_TEAM = "teamName"
const FIELD_PASSWORD = "contestPassword"
const PASSWORD_REQUIRED = "La contraseña de la maratón es obligatoria."

/** Diálogo para inscribir un equipo: nombre del equipo + contraseña de la maratón. */
export function JoinContestDialog({ open, onOpenChange, contestTitle, onJoin }: JoinContestDialogProps) {
  const [teamName, setTeamName] = useState("")
  const [contestPassword, setContestPassword] = useState("")
  // Errores que devuelve el backend (se limpian al editar el campo correspondiente).
  const [fieldErrors, setFieldErrors] = useState<ContestFieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  // Campos tocados: solo se muestra el error de validación local tras interactuar con ellos.
  const [touched, setTouched] = useState<{ team: boolean; password: boolean }>({ team: false, password: false })

  const teamNameError = validateTeamName(teamName)
  const passwordError = contestPassword ? null : PASSWORD_REQUIRED
  const canSubmit = teamNameError === null && passwordError === null && !isSubmitting

  const shownTeamError = fieldErrors[FIELD_TEAM] ?? (touched.team ? teamNameError : null)
  const shownPasswordError = fieldErrors[FIELD_PASSWORD] ?? (touched.password ? passwordError : null)

  const reset = () => {
    setTeamName("")
    setContestPassword("")
    setFieldErrors({})
    setServerError(null)
    setTouched({ team: false, password: false })
  }

  const handleOpenChange = (next: boolean) => {
    if (!next && isSubmitting) return
    if (!next) reset()
    onOpenChange(next)
  }

  /** Quita el error del backend de un campo cuando el usuario vuelve a escribir en él. */
  const clearFieldError = (field: string) => {
    setFieldErrors((prev) => {
      if (!(field in prev)) return prev
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!canSubmit) {
      setTouched({ team: true, password: true })
      return
    }

    setServerError(null)
    setIsSubmitting(true)
    try {
      await onJoin(teamName.trim(), contestPassword)
      reset()
      onOpenChange(false)
    } catch (err: unknown) {
      if (err instanceof ContestApiError) {
        const fromBody = err.fieldErrors ?? {}
        setFieldErrors(fromBody)
        // Errores de campo que no tienen input en este formulario se muestran como mensaje general.
        const unmapped = Object.entries(fromBody)
          .filter(([key]) => key !== FIELD_TEAM && key !== FIELD_PASSWORD)
          .map(([, msg]) => msg)
        setServerError(err.serverMessage ?? (unmapped.length > 0 ? unmapped.join(" ") : null) ?? "No se pudo completar la inscripción.")
      } else {
        setServerError("No hay conexión con el servidor. Inténtalo de nuevo.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="border-border bg-card sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-foreground">Inscribir equipo</DialogTitle>
          <DialogDescription>
            Inscríbete en <span className="text-foreground font-medium">{contestTitle}</span> con el nombre de tu equipo.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="team-name">Nombre del equipo</Label>
            <Input
              id="team-name"
              name={FIELD_TEAM}
              value={teamName}
              onChange={(e) => {
                setTeamName(e.target.value)
                clearFieldError(FIELD_TEAM)
              }}
              onBlur={() => setTouched((t) => ({ ...t, team: true }))}
              placeholder="Ej: Los Algoritmos"
              autoComplete="off"
              aria-invalid={Boolean(shownTeamError)}
              aria-describedby="team-name-help"
             
              disabled={isSubmitting}
              required
            />
            <p id="team-name-help" className="text-xs text-muted-foreground">
              Entre {TEAM_NAME_MIN_LENGTH} y {TEAM_NAME_MAX_LENGTH} caracteres. Tu equipo puede tener hasta{" "}
              {TEAM_MAX_MEMBERS} integrantes; podrás invitar a los demás después de inscribirte.
            </p>
            {shownTeamError && (
              <p className="text-sm text-destructive">{shownTeamError}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="contest-password">Contraseña de la maratón</Label>
            <Input
              id="contest-password"
              name={FIELD_PASSWORD}
              type="password"
              value={contestPassword}
              onChange={(e) => {
                setContestPassword(e.target.value)
                clearFieldError(FIELD_PASSWORD)
              }}
              onBlur={() => setTouched((t) => ({ ...t, password: true }))}
              autoComplete="off"
              aria-invalid={Boolean(shownPasswordError)}
              aria-describedby="contest-password-help"
             
              disabled={isSubmitting}
              required
            />
            <p id="contest-password-help" className="text-xs text-muted-foreground">
              Pídesela al organizador de la maratón si no la tienes.
            </p>
            {shownPasswordError && (
              <p className="text-sm text-destructive">{shownPasswordError}</p>
            )}
          </div>

          {serverError && (
            <Alert variant="destructive" role="alert">
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          <DialogFooter className="gap-2 sm:gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={isSubmitting}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={!canSubmit}
              className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {isSubmitting && <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" />}
              {isSubmitting ? "Inscribiendo…" : "Inscribirme"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
