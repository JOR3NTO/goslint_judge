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
import { validateTeamName } from "../utils/team-rules"

interface JoinContestDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  contestTitle: string
  /** Debe lanzar `ContestApiError` si el backend rechaza la inscripción. */
  onJoin: (teamName: string, contestPassword: string) => Promise<void>
}

const FIELD_TEAM = "teamName"
const FIELD_PASSWORD = "contestPassword"

/** Diálogo para inscribir un equipo: nombre del equipo + contraseña de la maratón. */
export function JoinContestDialog({ open, onOpenChange, contestTitle, onJoin }: JoinContestDialogProps) {
  const [teamName, setTeamName] = useState("")
  const [contestPassword, setContestPassword] = useState("")
  const [fieldErrors, setFieldErrors] = useState<ContestFieldErrors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const reset = () => {
    setTeamName("")
    setContestPassword("")
    setFieldErrors({})
    setServerError(null)
  }

  const handleOpenChange = (next: boolean) => {
    if (!next && isSubmitting) return
    if (!next) reset()
    onOpenChange(next)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (isSubmitting) return

    const localErrors: ContestFieldErrors = {}
    const teamNameError = validateTeamName(teamName)
    if (teamNameError) localErrors[FIELD_TEAM] = teamNameError
    if (!contestPassword) localErrors[FIELD_PASSWORD] = "La contraseña de la maratón es obligatoria."
    setFieldErrors(localErrors)
    setServerError(null)
    if (Object.keys(localErrors).length > 0) return

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
              onChange={(e) => setTeamName(e.target.value)}
              placeholder="Ej: Los Algoritmos"
              autoComplete="off"
              aria-invalid={Boolean(fieldErrors[FIELD_TEAM])}
              aria-describedby="team-name-help"
              className="bg-input border-border focus:border-primary"
              disabled={isSubmitting}
            />
            <p id="team-name-help" className="text-xs text-muted-foreground">
              El equipo puede tener de 1 a 3 integrantes; tras crearlo podrás invitar a los demás.
            </p>
            {fieldErrors[FIELD_TEAM] && (
              <p className="text-sm text-destructive">{fieldErrors[FIELD_TEAM]}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="contest-password">Contraseña de la maratón</Label>
            <Input
              id="contest-password"
              name={FIELD_PASSWORD}
              type="password"
              value={contestPassword}
              onChange={(e) => setContestPassword(e.target.value)}
              autoComplete="off"
              aria-invalid={Boolean(fieldErrors[FIELD_PASSWORD])}
              className="bg-input border-border focus:border-primary"
              disabled={isSubmitting}
            />
            {fieldErrors[FIELD_PASSWORD] && (
              <p className="text-sm text-destructive">{fieldErrors[FIELD_PASSWORD]}</p>
            )}
          </div>

          {serverError && (
            <Alert variant="destructive">
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}

          <DialogFooter className="gap-2 sm:gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
              disabled={isSubmitting}
              className="border-border hover:border-primary hover:bg-primary/10"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting}
              className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
              Inscribirme
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
