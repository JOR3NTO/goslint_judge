"use client"

import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Alert, AlertDescription, AlertTitle } from "@/shared/ui/primitives/alert"
import { Button } from "@/shared/ui/primitives/button"
import { AlertTriangle, Info, RefreshCw } from "lucide-react"
import { useProfile } from "../hooks/use-profile"
import { ProfileHeader } from "./profile-header"
import { ProfileDetails } from "./profile-details"
import { ProfileSkeleton } from "./profile-skeleton"

export function ProfilePage() {
  const { result, isLoading, error, reload } = useProfile()

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Navbar />
      <main className="flex-1 pt-24 pb-16">
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 space-y-6">
          <h1 className="sr-only">Mi perfil</h1>

          {isLoading && <ProfileSkeleton />}

          {!isLoading && error && (
            <Alert variant="destructive">
              <AlertTriangle className="h-4 w-4" />
              <AlertTitle>No pudimos cargar tu perfil</AlertTitle>
              <AlertDescription className="flex items-center justify-between gap-4">
                <span>{error}</span>
                <Button variant="outline" size="sm" onClick={reload} className="gap-2 shrink-0">
                  <RefreshCw className="h-4 w-4" />
                  Reintentar
                </Button>
              </AlertDescription>
            </Alert>
          )}

          {!isLoading && !error && result && (
            <>
              {!result.isComplete && (
                <Alert>
                  <Info className="h-4 w-4" />
                  <AlertTitle>Perfil parcial</AlertTitle>
                  <AlertDescription>
                    Solo se muestran los datos de tu sesión. El resto de tu información
                    estará disponible cuando el servicio de perfil esté activo.
                  </AlertDescription>
                </Alert>
              )}
              <ProfileHeader profile={result.profile} />
              <ProfileDetails profile={result.profile} />
            </>
          )}
        </div>
      </main>
      <Footer />
    </div>
  )
}
