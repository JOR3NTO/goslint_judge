"use client"

import { Alert, AlertDescription, AlertTitle } from "@/shared/ui/primitives/alert"
import { Button } from "@/shared/ui/primitives/button"
import { AlertTriangle, RefreshCw } from "lucide-react"

interface ContestsLoadErrorProps {
  message: string
  onRetry: () => void
}

/** Mensaje de error con botón para reintentar la carga. */
export function ContestsLoadError({ message, onRetry }: ContestsLoadErrorProps) {
  return (
    <Alert variant="destructive">
      <AlertTriangle className="h-4 w-4" />
      <AlertTitle>No pudimos cargar las maratones</AlertTitle>
      <AlertDescription className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <span>{message}</span>
        <Button variant="outline" size="sm" onClick={onRetry} className="gap-2 shrink-0">
          <RefreshCw className="h-4 w-4" />
          Reintentar
        </Button>
      </AlertDescription>
    </Alert>
  )
}
