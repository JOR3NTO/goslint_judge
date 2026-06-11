import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { HeroSection } from "@/components/hero-section"
import { FeaturesSection } from "@/components/features-section"
import { ContestCard, type Contest } from "@/components/contest-card"
import { Button } from "@/components/ui/button"
import { ArrowRight } from "lucide-react"
import Link from "next/link"

const upcomingContests: Contest[] = [
  {
    id: "1",
    title: "Goslint Weekly Challenge #42",
    description: "Competencia semanal con problemas de estructuras de datos y algoritmos de grafos.",
    startDate: "15 Ene 2025",
    endDate: "15 Ene 2025",
    duration: "3 horas",
    participants: 234,
    difficulty: "Intermedio",
    status: "upcoming",
    problems: 6
  },
  {
    id: "2",
    title: "Maratón Nacional de Algoritmos",
    description: "La competencia más grande del año. Premios especiales para los top 10.",
    startDate: "20 Ene 2025",
    endDate: "20 Ene 2025",
    duration: "5 horas",
    participants: 1205,
    difficulty: "Avanzado",
    status: "active",
    problems: 10
  },
  {
    id: "3",
    title: "Práctica: Programación Dinámica",
    description: "Sesión de práctica enfocada en problemas clásicos de programación dinámica.",
    startDate: "18 Ene 2025",
    endDate: "18 Ene 2025",
    duration: "2 horas",
    participants: 89,
    difficulty: "Principiante",
    status: "upcoming",
    problems: 5
  }
]

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main>
        <HeroSection />
        <FeaturesSection />
        
        {/* Upcoming Contests Section */}
        <section className="py-24 relative">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="flex items-center justify-between mb-12">
              <div>
                <h2 className="text-3xl font-bold text-foreground mb-2">
                  Próximas <span className="text-primary">Maratones</span>
                </h2>
                <p className="text-muted-foreground">
                  Únete a las próximas competencias y pon a prueba tus habilidades.
                </p>
              </div>
              <Link href="/contests" className="hidden sm:block">
                <Button variant="outline" className="gap-2 border-border hover:border-primary hover:bg-primary/10">
                  Ver Todas
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {upcomingContests.map((contest) => (
                <ContestCard key={contest.id} contest={contest} />
              ))}
            </div>

            <div className="mt-8 sm:hidden text-center">
              <Link href="/contests">
                <Button variant="outline" className="gap-2 border-border hover:border-primary hover:bg-primary/10">
                  Ver Todas las Maratones
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="py-24 relative">
          <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-primary/5 to-transparent" />
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto space-y-6">
              <h2 className="text-3xl sm:text-4xl font-bold text-foreground">
                ¿Listo para el <span className="text-primary text-glow">desafío</span>?
              </h2>
              <p className="text-lg text-muted-foreground">
                Únete a la comunidad de programadores competitivos más grande. 
                Mejora tus habilidades, compite con los mejores y alcanza nuevas metas.
              </p>
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
                <Link href="/register">
                  <Button size="lg" className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green px-8">
                    Crear Cuenta Gratis
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                </Link>
                <Link href="/contests">
                  <Button size="lg" variant="outline" className="gap-2 border-border hover:border-primary hover:bg-primary/10 px-8">
                    Explorar Maratones
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
