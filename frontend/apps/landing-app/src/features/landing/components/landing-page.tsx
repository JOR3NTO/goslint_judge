import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { HeroSection } from "./hero-section"
import { FeaturesSection } from "./features-section"
import { ContestsPreview } from "./contests-preview"
import { env } from "@/core/config/env"
import { Button } from "@/shared/ui/primitives/button"
import { ArrowRight } from "lucide-react"

export function LandingPage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main>
        <HeroSection />
        <FeaturesSection />
        <ContestsPreview />

        {/* CTA final */}
        <section className="relative py-24">
          <div className="relative mx-auto max-w-3xl space-y-6 px-4 text-center sm:px-6 lg:px-8">
            <h2 className="text-3xl font-bold text-foreground sm:text-4xl">
              ¿Listo para el <span className="text-primary">desafío</span>?
            </h2>
            <p className="text-lg text-muted-foreground">
              Crea tu cuenta para participar en las maratones publicadas. Si ya tienes una, inicia
              sesión.
            </p>
            <div className="flex flex-col items-center justify-center gap-4 pt-4 sm:flex-row">
              <Button
                asChild
                size="lg"
                className="glow-green h-12 w-full bg-primary px-8 text-base text-primary-foreground hover:bg-primary/90 sm:w-56"
              >
                <a href={`${env.APP_URL}/register`}>
                  Crear cuenta
                  <ArrowRight className="h-5 w-5" aria-hidden="true" />
                </a>
              </Button>
              <a
                href={`${env.APP_URL}/login`}
                className="rounded-md text-sm font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
              >
                Ya tengo cuenta: iniciar sesión
              </a>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
