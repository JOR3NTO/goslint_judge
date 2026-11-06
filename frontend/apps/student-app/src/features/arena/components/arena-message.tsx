import type { ReactNode } from "react"
import { Card, CardContent } from "@/shared/ui/primitives/card"
import { Clock } from "lucide-react"

/** Mensaje centrado para estados de acceso (no inscrito, error, etc.). */
export function ArenaMessage({
  icon,
  title,
  description,
  actions,
}: {
  icon: ReactNode
  title: string
  description: string
  actions?: ReactNode
}) {
  return (
    <Card className="mx-auto max-w-xl border-border bg-card">
      <CardContent className="flex flex-col items-center gap-4 py-10 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-full border border-primary/30 bg-primary/10 text-primary">
          {icon}
        </div>
        <h2 className="text-xl font-semibold text-foreground">{title}</h2>
        <p className="max-w-md text-muted-foreground">{description}</p>
        {actions && <div className="flex flex-wrap justify-center gap-3">{actions}</div>}
      </CardContent>
    </Card>
  )
}

/** Estado vacío de las pestañas que llegan en otra tarea. */
export function ComingSoon({ title }: { title: string }) {
  return (
    <Card className="border-dashed border-border bg-card/50">
      <CardContent className="flex flex-col items-center gap-3 py-16 text-center">
        <Clock className="h-8 w-8 text-primary" />
        <h2 className="text-lg font-semibold text-foreground">{title}</h2>
        <p className="text-muted-foreground">Próximamente</p>
      </CardContent>
    </Card>
  )
}
