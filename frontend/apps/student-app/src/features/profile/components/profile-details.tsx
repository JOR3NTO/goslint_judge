import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/primitives/card"
import { Building2, CalendarDays, Mail, User, AtSign, CircleCheck } from "lucide-react"
import type { ProfileResult } from "../types/profile.types"
import { formatDate } from "../utils/format"

interface DetailRowProps {
  icon: React.ComponentType<{ className?: string }>
  label: string
  value: string | null | undefined
}

function DetailRow({ icon: Icon, label, value }: DetailRowProps) {
  return (
    <div className="flex items-start gap-3 py-3">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 border border-primary/20">
        <Icon className="h-4 w-4 text-primary" />
      </div>
      <div className="min-w-0">
        <dt className="text-sm text-muted-foreground">{label}</dt>
        <dd className="text-foreground break-words">
          {value ? value : <span className="text-muted-foreground italic">No disponible</span>}
        </dd>
      </div>
    </div>
  )
}

export function ProfileDetails({ profile }: { profile: ProfileResult["profile"] }) {
  return (
    <Card className="border-border bg-card/50 backdrop-blur">
      <CardHeader>
        <CardTitle className="text-lg">Información de la cuenta</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid sm:grid-cols-2 gap-x-6 divide-y sm:divide-y-0 divide-border">
          <DetailRow icon={User} label="Nombre" value={profile.firstName} />
          <DetailRow icon={User} label="Apellido" value={profile.lastName} />
          <DetailRow icon={AtSign} label="Nombre de usuario" value={profile.username} />
          <DetailRow icon={Mail} label="Correo electrónico" value={profile.email} />
          <DetailRow icon={Building2} label="Institución" value={profile.institution} />
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
