import { Avatar, AvatarFallback, AvatarImage } from "@/shared/ui/primitives/avatar"
import { Badge } from "@/shared/ui/primitives/badge"
import { Card, CardContent } from "@/shared/ui/primitives/card"
import { ShieldCheck, GraduationCap } from "lucide-react"
import type { ProfileResult } from "../types/profile.types"
import { getAvatarDataUri } from "../utils/avatar"
import { getDisplayName, getInitials, ROLE_LABELS } from "../utils/format"

export function ProfileHeader({ profile }: { profile: ProfileResult["profile"] }) {
  const name = getDisplayName(profile)
  // La semilla es el username; si el perfil es parcial (solo JWT) se usa el id.
  const avatarSrc = getAvatarDataUri(profile.username ?? profile.id)
  const RoleIcon = profile.role === "ADMIN" ? ShieldCheck : GraduationCap

  return (
    <Card className="border-border bg-card/50 backdrop-blur overflow-hidden">
      <div className="h-24 bg-gradient-to-r from-primary/30 via-primary/10 to-transparent" />
      <CardContent className="-mt-12 pb-6 flex flex-col sm:flex-row sm:items-end gap-4">
        <Avatar className="h-24 w-24 border-4 border-background bg-card">
          <AvatarImage src={avatarSrc} alt={`Avatar de ${name}`} />
          <AvatarFallback className="bg-primary/15 text-primary text-3xl font-bold">
            {getInitials(profile)}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1 min-w-0 space-y-1">
          <h2 className="text-2xl font-bold text-foreground truncate">{name}</h2>
          {profile.username && (
            <p className="text-muted-foreground">@{profile.username}</p>
          )}
        </div>
        <Badge variant="outline" className="gap-1.5 border-primary/40 text-primary w-fit">
          <RoleIcon className="h-3.5 w-3.5" />
          {ROLE_LABELS[profile.role]}
        </Badge>
      </CardContent>
    </Card>
  )
}
