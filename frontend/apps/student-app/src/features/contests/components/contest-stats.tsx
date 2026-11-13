import type { ReactNode } from "react"

export interface ContestStatItem {
  label: string
  value: ReactNode
}

interface ContestStatsProps {
  items: ContestStatItem[]
}

/** Tira compacta de métricas (una fila en escritorio, apiladas en móvil). */
export function ContestStats({ items }: ContestStatsProps) {
  return (
    <dl className={`grid grid-cols-1 gap-3 ${items.length >= 3 ? "sm:grid-cols-3" : "sm:grid-cols-2"}`}>
      {items.map((item) => (
        <div
          key={item.label}
          className="flex items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3"
        >
          <dt className="text-sm text-muted-foreground">{item.label}</dt>
          <dd className="min-w-0 truncate text-right text-xl font-semibold tabular-nums text-foreground">
            {item.value}
          </dd>
        </div>
      ))}
    </dl>
  )
}
