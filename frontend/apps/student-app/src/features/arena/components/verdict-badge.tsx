import { Badge } from "@/shared/ui/primitives/badge"
import { cn } from "@/shared/utils/cn"
import type { SubmissionDTO } from "../types/arena.types"
import { submissionStateLabel, submissionStateTone } from "../utils/verdict"

/** Etiqueta de estado/veredicto de un envío (con colores del tema). */
export function SubmissionStateBadge({
  submission,
  className,
}: {
  submission: Pick<SubmissionDTO, "status" | "verdict">
  className?: string
}) {
  return (
    <Badge
      variant="outline"
      className={cn("whitespace-nowrap font-medium", submissionStateTone(submission), className)}
    >
      {submissionStateLabel(submission)}
    </Badge>
  )
}
