import { Card, CardContent } from "@/shared/ui/primitives/card"
import { Skeleton } from "@/shared/ui/primitives/skeleton"

export function ProfileSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Cargando perfil">
      <Card className="border-border overflow-hidden">
        <Skeleton className="h-24 rounded-none" />
        <CardContent className="-mt-12 pb-6 flex items-end gap-4">
          <Skeleton className="h-24 w-24 rounded-full border-4 border-background" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-7 w-48" />
            <Skeleton className="h-4 w-32" />
          </div>
        </CardContent>
      </Card>
      <Card className="border-border">
        <CardContent className="grid sm:grid-cols-2 gap-6 pt-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
