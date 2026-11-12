"use client"

import { useEffect, useState } from "react"
import { Alert, AlertDescription, AlertTitle } from "@/shared/ui/primitives/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/primitives/alert-dialog"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { UserCombobox } from "./user-combobox"
import { Input } from "@/shared/ui/primitives/input"
import { Label } from "@/shared/ui/primitives/label"
import { Crown, Loader2, LogOut, Mail, Pencil, UserMinus, Users } from "lucide-react"
import { useTeamActions } from "../hooks/use-team-actions"
import type { ContestDTO, InvitationDTO, InvitationStatus, TeamDTO, UserSummaryDTO } from "../types/contest.types"
import {
  TEAM_MAX_MEMBERS,
  isRegistrationOpen,
  shortId,
  validateTeamName,
} from "../utils/team-rules"

interface MyTeamPanelProps {
  contest: ContestDTO
  team: TeamDTO
  /** Id del usuario autenticado (claim `sub`), o `null` si no se pudo leer la sesión. */
  currentUserId: string | null
  onTeamChanged: (team: TeamDTO) => void
  onTeamLeft: (teamId: string) => void
}

const INVITATION_STATUS_LABELS: Record<InvitationStatus, string> = {
  PENDING: "pendiente",
  ACCEPTED: "aceptada",
  REJECTED: "rechazada",
  EXPIRED: "vencida",
}

/**
 * Panel "Mi equipo" del detalle de la maratón. El capitán puede invitar, renombrar y quitar
 * integrantes; los demás solo pueden abandonar. Las reglas reales están en el backend
 * (ver team-rules.ts); aquí solo se reflejan para deshabilitar controles.
 */
export function MyTeamPanel({ contest, team, currentUserId, onTeamChanged, onTeamLeft }: MyTeamPanelProps) {
  const actions = useTeamActions({ onTeamChanged, onTeamLeft })
  const busy = actions.pending !== null

  const isCaptain = currentUserId !== null && team.captainId === currentUserId
  const memberCount = team.members.length
  const isFull = memberCount >= TEAM_MAX_MEMBERS
  const ended = contest.status === "ENDED"
  const registrationOpen = isRegistrationOpen(contest)
  const canInvite = isCaptain && registrationOpen && !isFull
  const canEdit = isCaptain && !ended

  // Formulario de invitación
  const [inviteUser, setInviteUser] = useState<UserSummaryDTO | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [inviteSent, setInviteSent] = useState<InvitationDTO | null>(null)

  // Renombrar
  const [newName, setNewName] = useState(team.teamName)
  const [nameError, setNameError] = useState<string | null>(null)
  useEffect(() => {
    setNewName(team.teamName)
  }, [team.teamName])

  // Diálogos de confirmación
  const [memberToRemove, setMemberToRemove] = useState<string | null>(null)
  const [isLeaveOpen, setIsLeaveOpen] = useState(false)

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!canInvite || busy) return

    setInviteSent(null)
    if (!inviteUser) {
      setInviteError("Selecciona al estudiante que quieres invitar.")
      return
    }
    setInviteError(null)

    try {
      const invitation = await actions.invite(team.id, inviteUser.id)
      setInviteSent(invitation)
      setInviteUser(null)
    } catch {
      // El error ya queda en `actions.error`.
    }
  }

  const handleRename = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!canEdit || busy) return
    const validation = validateTeamName(newName)
    setNameError(validation)
    if (validation) return
    try {
      await actions.rename(team.id, newName.trim())
    } catch {
      // Error visible en `actions.error`.
    }
  }

  const confirmRemove = async () => {
    const userId = memberToRemove
    if (!userId) return
    try {
      await actions.removeMember(team.id, userId)
    } catch {
      // Error visible en la tarjeta.
    }
    setMemberToRemove(null)
  }

  const confirmLeave = async () => {
    if (!currentUserId) return
    try {
      await actions.leave(team.id, currentUserId)
    } catch {
      // Error visible en la tarjeta.
    }
    setIsLeaveOpen(false)
  }

  // Motivo por el que el capitán no puede invitar (texto de ayuda).
  let inviteBlockedReason: string | null = null
  if (ended) inviteBlockedReason = "La maratón ya terminó: no se pueden enviar invitaciones."
  else if (!registrationOpen) inviteBlockedReason = "Las invitaciones se cierran cuando inicia la maratón."
  else if (isFull) inviteBlockedReason = `El equipo ya tiene ${TEAM_MAX_MEMBERS} integrantes.`

  return (
    <Card className="border-primary/30 bg-card">
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2 text-foreground">
          <Users className="h-5 w-5 text-primary" />
          Mi equipo
          <span className="text-sm font-normal text-muted-foreground">
            ({memberCount}/{TEAM_MAX_MEMBERS} integrantes)
          </span>
        </CardTitle>
        <CardDescription>
          {isCaptain
            ? "Eres el capitán: solo tú puedes invitar integrantes y editar el equipo."
            : "Eres integrante de este equipo."}
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-6">
        {actions.error && (
          <Alert variant="destructive">
            <AlertTitle>No se pudo completar la acción</AlertTitle>
            <AlertDescription>{actions.error}</AlertDescription>
          </Alert>
        )}

        {/* Nombre y lista de integrantes */}
        <div className="space-y-3">
          <p className="text-lg font-semibold text-foreground break-words">{team.teamName}</p>

          {team.members.length === 0 ? (
            <p className="text-sm text-muted-foreground">El equipo aún no tiene integrantes registrados.</p>
          ) : (
            <ul className="divide-y divide-border rounded-md border border-border">
              {team.members.map((member) => {
                const isMe = member.userId === currentUserId
                const isTeamCaptain = member.userId === team.captainId
                return (
                  <li key={member.id} className="flex items-center justify-between gap-3 px-3 py-2">
                    <div className="flex min-w-0 flex-wrap items-center gap-2">
                      <span className="font-medium text-foreground">
                        {isMe ? "Tú" : "Integrante"}
                      </span>
                      <span className="font-mono text-xs text-muted-foreground" title={member.userId}>
                        {shortId(member.userId)}
                      </span>
                      {isTeamCaptain && (
                        <Badge variant="outline" className="gap-1 border-primary/30 text-primary">
                          <Crown className="h-3 w-3" />
                          Capitán
                        </Badge>
                      )}
                    </div>
                    {canEdit && !isTeamCaptain && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={busy}
                        onClick={() => setMemberToRemove(member.userId)}
                        className="gap-2 border-border hover:border-destructive hover:bg-destructive/10"
                      >
                        <UserMinus className="h-4 w-4" />
                        Quitar
                      </Button>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </div>

        {isCaptain ? (
          <>
            {/* Invitar integrante */}
            <section className="space-y-3 border-t border-border pt-5" aria-labelledby="invite-heading">
              <h3 id="invite-heading" className="flex items-center gap-2 font-semibold text-foreground">
                <Mail className="h-4 w-4 text-primary" />
                Invitar integrante
              </h3>

              {inviteBlockedReason && (
                <p className="text-sm text-muted-foreground">{inviteBlockedReason}</p>
              )}

              <form onSubmit={handleInvite} noValidate className="space-y-3">
                <div className="space-y-2">
                  <Label htmlFor="invite-user">Estudiante</Label>
                  <UserCombobox
                    id="invite-user"
                    value={inviteUser}
                    onChange={(user) => {
                      setInviteUser(user)
                      setInviteError(null)
                    }}
                    disabled={!canInvite || busy}
                    invalid={Boolean(inviteError)}
                  />
                  {inviteError && <p className="text-sm text-destructive">{inviteError}</p>}
                </div>

                <Button
                  type="submit"
                  disabled={!canInvite || busy}
                  className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
                >
                  {actions.pending === "invite" && <Loader2 className="h-4 w-4 animate-spin" />}
                  Enviar invitación
                </Button>
              </form>

              {inviteSent && (
                <Alert>
                  <AlertTitle>Invitación enviada</AlertTitle>
                  <AlertDescription>
                    Se envió a {inviteSent.invitedEmail}. Estado: {INVITATION_STATUS_LABELS[inviteSent.status]}.
                  </AlertDescription>
                </Alert>
              )}
            </section>

            {/* Editar nombre */}
            <section className="space-y-3 border-t border-border pt-5" aria-labelledby="rename-heading">
              <h3 id="rename-heading" className="flex items-center gap-2 font-semibold text-foreground">
                <Pencil className="h-4 w-4 text-primary" />
                Nombre del equipo
              </h3>
              <form onSubmit={handleRename} noValidate className="flex flex-col gap-2 sm:flex-row">
                <div className="flex-1 space-y-2">
                  <Label htmlFor="rename-team" className="sr-only">
                    Nuevo nombre del equipo
                  </Label>
                  <Input
                    id="rename-team"
                    value={newName}
                    onChange={(e) => {
                      setNewName(e.target.value)
                      setNameError(null)
                    }}
                    disabled={!canEdit || busy}
                    aria-invalid={Boolean(nameError)}
                    className="bg-input border-border focus:border-primary"
                  />
                  {nameError && <p className="text-sm text-destructive">{nameError}</p>}
                </div>
                <Button
                  type="submit"
                  variant="outline"
                  disabled={!canEdit || busy || newName.trim() === team.teamName}
                  className="gap-2 border-border hover:border-primary hover:bg-primary/10"
                >
                  {actions.pending === "rename" && <Loader2 className="h-4 w-4 animate-spin" />}
                  Guardar nombre
                </Button>
              </form>
            </section>

            <p className="text-xs text-muted-foreground border-t border-border pt-4">
              El capitán no puede abandonar el equipo desde aquí.
            </p>
          </>
        ) : (
          <section className="space-y-3 border-t border-border pt-5">
            <p className="text-sm text-muted-foreground">
              Solo el capitán puede invitar integrantes y cambiar el nombre del equipo.
            </p>
            <Button
              type="button"
              variant="outline"
              disabled={ended || busy || !currentUserId}
              onClick={() => setIsLeaveOpen(true)}
              className="gap-2 border-destructive/40 text-destructive hover:bg-destructive/10 hover:text-destructive"
            >
              {actions.pending === "leave" ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <LogOut className="h-4 w-4" />
              )}
              Abandonar equipo
            </Button>
            {ended && <p className="text-xs text-muted-foreground">La maratón ya terminó.</p>}
          </section>
        )}
      </CardContent>

      {/* Confirmar quitar integrante */}
      <AlertDialog open={memberToRemove !== null} onOpenChange={(open) => !open && setMemberToRemove(null)}>
        <AlertDialogContent className="border-border bg-card">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Quitar integrante?</AlertDialogTitle>
            <AlertDialogDescription>
              Se quitará al integrante {memberToRemove ? shortId(memberToRemove) : ""} del equipo.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                void confirmRemove()
              }}
              disabled={busy}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {actions.pending === "remove" && <Loader2 className="h-4 w-4 animate-spin" />}
              Quitar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Confirmar abandonar equipo */}
      <AlertDialog open={isLeaveOpen} onOpenChange={(open) => !busy && setIsLeaveOpen(open)}>
        <AlertDialogContent className="border-border bg-card">
          <AlertDialogHeader>
            <AlertDialogTitle>¿Abandonar el equipo?</AlertDialogTitle>
            <AlertDialogDescription>
              Dejarás de ser integrante de {team.teamName} en esta maratón.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                void confirmLeave()
              }}
              disabled={busy}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {actions.pending === "leave" && <Loader2 className="h-4 w-4 animate-spin" />}
              Abandonar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Card>
  )
}
