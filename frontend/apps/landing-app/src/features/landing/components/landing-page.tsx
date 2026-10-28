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

        {/* CTA Final */}
        <section className="py-24 relative">
          <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-primary/5 to-transparent" />
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto space-y-6">
              <h2 className="text-3xl sm:text-4xl font-bold text-foreground">
                ¿Listo para el <span className="text-primary text-glow">desafío</span>?
              </h2>
              <p className="text-lg text-muted-foreground">
                Únete a la comunidad de programadores competitivos.
                Mejora tus habilidades, compite con los mejores y alcanza nuevas metas.
              </p>
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
                <a href={`${env.APP_URL}/register`}>
                  <Button
                    size="lg"
                    className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green px-8"
                  >
                    Crear Cuenta Gratis
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                </a>
                <a href={`${env.APP_URL}/login`}>
                  <Button
                    size="lg"
                    variant="outline"
                    className="gap-2 border-border hover:border-primary hover:bg-primary/10 px-8"
                  >
                    Iniciar Sesión
                  </Button>
                </a>
              </div>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
