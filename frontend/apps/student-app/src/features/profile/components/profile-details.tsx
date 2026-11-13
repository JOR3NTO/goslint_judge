import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Building2, CalendarDays, Mail, User, AtSign, CircleCheck } from "lucide-react"
import type { ProfileResult } from "../types/profile.types"
import { formatDate } from "../utils/format"

interface DetailRowProps {
  icon: React.ComponentType<{ className?: string }>
  label: string
  value: string | null | undefined
  /** Texto cuando no hay valor. Por defecto "No disponible". */
  emptyText?: string
}

function DetailRow({ icon: Icon, label, value, emptyText = "No disponible" }: DetailRowProps) {
  return (
    <div className="flex items-start gap-3 py-3">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-muted border border-border">
        <Icon className="h-4 w-4 text-muted-foreground" />
      </div>
      <div className="min-w-0">
        <dt className="text-sm text-muted-foreground">{label}</dt>
        <dd className="text-foreground break-words">
          {value ? value : <span className="text-muted-foreground italic">{emptyText}</span>}
        </dd>
      </div>
    </div>
  )
}

export function ProfileDetails({ profile }: { profile: ProfileResult["profile"] }) {
  // undefined: el perfil viene solo del JWT (fallback), no sabemos la institución.
  // null/"": el perfil completo y el usuario no indicó institución en el registro.
  const institution = profile.institution?.trim() || null
  const institutionEmpty = profile.institution === undefined ? "No disponible" : "No especificada"

  return (
    <Card className="border-border bg-card shadow-sm">
      <CardHeader>
        <CardTitle className="text-lg">Información de la cuenta</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid sm:grid-cols-2 gap-x-6 divide-y sm:divide-y-0 divide-border">
          <DetailRow icon={User} label="Nombre" value={profile.firstName} />
          <DetailRow icon={User} label="Apellido" value={profile.lastName} />
          <DetailRow icon={AtSign} label="Nombre de usuario" value={profile.username} />
          <DetailRow icon={Mail} label="Correo electrónico" value={profile.email} />
          <DetailRow
            icon={Building2}
            label="Institución"
            value={institution}
            emptyText={institutionEmpty}
          />
          <DetailRow
            icon={CalendarDays}
            label="Miembro desde"
            value={profile.createdAt ? formatDate(profile.createdAt) : null}
          />
          {profile.isActive !== undefined && (
            <DetailRow
              icon={CircleCheck}
              label="Estado de la cuenta"
              value={profile.isActive ? "Activa" : "Inactiva"}
            />
          )}
        </dl>
      </CardContent>
    </Card>
  )
}
