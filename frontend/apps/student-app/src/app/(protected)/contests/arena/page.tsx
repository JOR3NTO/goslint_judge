import { Suspense } from "react"
import { ContestArenaPage } from "@/features/arena"

export default function ContestArenaRoute() {
  return (
    <Suspense>
      <ContestArenaPage />
    </Suspense>
  )
}
