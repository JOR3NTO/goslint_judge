"use client"

import Link from "next/link"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Alert, AlertDescription, AlertTitle } from "@/shared/ui/primitives/alert"
import { Badge } from "@/shared/ui/primitives/badge"
import { Button } from "@/shared/ui/primitives/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { AlertTriangle, ArrowRight, Loader2, Mail, RefreshCw } from "lucide-react"
import { useMyInvitations, type InvitationItem } from "../hooks/use-my-invitations"
import type { InvitationStatus } from "../types/contest.types"
import { formatDateTime } from "../utils/format"
import { TEAM_MAX_MEMBERS, isRegistrationOpen } from "../utils/team-rules"

const statusBadge: Record<InvitationStatus, { label: string; className: string }> = {
  PENDING: { label: "Pendiente", className: "bg-info/10 text-info border-info/30" },
  ACCEPTED: { label: "Aceptada", className: "bg-primary/10 text-primary border-primary/30" },
  REJECTED: { label: "Rechazada", className: "bg-muted text-muted-foreground border-muted" },
  EXPIRED: { label: "Vencida", className: "bg-muted text-muted-foreground border-muted" },
}

/** Lista de invitaciones recibidas, con aceptar / rechazar para las PENDING. */
export function InvitationsPage() {
  const { items, isLoading, error, reload, accept, reject, busyId, actionErrors } = useMyInvitations()

  const handleAccept = async (invitationId: string) => {
    try {
      await accept(invitationId)
    } catch {
      // El error queda en actionErrors[invitationId].
    }
  }

  const handleReject = async (invitationId: string) => {
    try {
      await reject(invitationId)
    } catch {
      // El error queda en actionErrors[invitationId].
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-24 pb-16">
        <div className="mx-auto max-w-4xl space-y-6 px-4 sm:px-6 lg:px-8">
          <header className="space-y-2">
            <h1 className="flex items-center gap-3 text-3xl font-bold text-foreground">
              <Mail className="h-7 w-7 text-primary" />
              Mis invitaciones
            </h1>
            <p className="text-muted-foreground">
              Invitaciones de capitanes para unirte a su equipo en una maratón.
            </p>
          </header>

          {error && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>No pudimos cargar tus invitaciones</AlertTitle>
              <AlertDescription className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <span>{error}</span>
                <Button variant="outline" size="sm" onClick={reload} className="shrink-0 gap-2">
                  <RefreshCw className="h-4 w-4" />
                  Reintentar
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {items === null && isLoading && !error && <InvitationsSkeleton />}

          {items !== null && items.length === 0 && (
            <Card className="border-border bg-card">
              <CardContent className="py-10 text-center text-muted-foreground">
                Aún no tienes invitaciones. Cuando un capitán te invite, aparecerá aquí.
              </CardContent>
            </Card>
          )}

          {items !== null && items.length > 0 && (
            <ul className="space-y-4">
              {items.map((item) => (
                <li key={item.invitation.id}>
                  <InvitationCard
                    item={item}
                    isBusy={busyId === item.invitation.id}
                    anyBusy={busyId !== null}
                    actionError={actionErrors[item.invitation.id] ?? null}
                    onAccept={() => handleAccept(item.invitation.id)}
                    onReject={() => handleReject(item.invitation.id)}
                  />
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>
      <Footer />
    </div>
  )
}

interface InvitationCardProps {
  item: InvitationItem
  isBusy: boolean
  anyBusy: boolean
  actionError: string | null
  onAccept: () => void
  onReject: () => void
}

function InvitationCard({ item, isBusy, anyBusy, actionError, onAccept, onReject }: InvitationCardProps) {
  const { invitation, team, contest } = item
  const badge = statusBadge[invitation.status]
  const isPending = invitation.status === "PENDING"
  const registrationClosed = contest !== null && !isRegistrationOpen(contest)
  // Espejo de AcceptInvitationUseCase: no se acepta una vez iniciada la maratón.
  const acceptBlockedReason =
    isPending && registrationClosed ? "La maratón ya inició: ya no se pueden aceptar invitaciones." : null

  return (
    <Card className="border-border bg-card">
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0 space-y-1">
            <CardTitle className="break-words text-foreground">
              {team ? team.teamName : "Equipo no disponible"}
            </CardTitle>
            <CardDescription>
              {contest ? (
                <Link href={`/contests/${contest.id}`} className="text-primary hover:underline">
                  {contest.title}
                </Link>
              ) : (
                "Maratón no disponible"
              )}
            </CardDescription>
          </div>
          <Badge variant="outline" className={badge.className}>
            {badge.label}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="space-y-1 text-sm text-muted-foreground">
          <p>
            Enviada a <span className="text-foreground">{invitation.invitedEmail}</span> el{" "}
            {formatDateTime(invitation.createdAt)}
          </p>
          {team && (
            <p>
              Integrantes: {team.members.length}/{TEAM_MAX_MEMBERS}
            </p>
          )}
          {invitation.status === "EXPIRED" && (
            <p>La invitación venció: el equipo alcanzó el máximo de {TEAM_MAX_MEMBERS} integrantes.</p>
          )}
          {acceptBlockedReason && <p>{acceptBlockedReason}</p>}
        </div>

        {actionError && (
          <Alert variant="destructive">
            <AlertDescription>{actionError}</AlertDescription>
          </Alert>
        )}

        <div className="flex flex-wrap gap-2">
          {isPending && (
            <>
              <Button
                type="button"
                onClick={onAccept}
                disabled={anyBusy || Boolean(acceptBlockedReason)}
                className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
              >
                {isBusy && <Loader2 className="h-4 w-4 animate-spin" />}
                Aceptar
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={onReject}
                disabled={anyBusy}
                className="border-border hover:border-destructive hover:bg-destructive/10"
              >
                Rechazar
              </Button>
            </>
          )}

          {invitation.status === "ACCEPTED" && contest && (
            <Link href={`/contests/${contest.id}`}>
              <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90">
                Ir a la maratón
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

function InvitationsSkeleton() {
  return (
    <div className="space-y-4" aria-busy="true" aria-label="Cargando invitaciones">
      <Skeleton className="h-36 w-full" />
      <Skeleton className="h-36 w-full" />
    </div>
  )
}
