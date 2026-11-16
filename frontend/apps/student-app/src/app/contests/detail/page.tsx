import { Suspense } from "react"
import { ContestDetailPage } from "@/features/contests"

export default function ContestDetailRoute() {
  return (
    <Suspense>
      <ContestDetailPage />
    </Suspense>
  )
}
