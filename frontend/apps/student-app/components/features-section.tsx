import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { 
  Trophy, 
  Sparkles, 
  Clock, 
  Users, 
  Code2, 
  Zap,
  Shield,
  LineChart
} from "lucide-react"

const features = [
  {
    icon: Trophy,
    title: "Maratones en Vivo",
    description: "Compite en tiempo real contra programadores de todo el mundo en emocionantes competencias de algoritmos."
  },
  {
    icon: Sparkles,
    title: "AI Feedback",
    description: "Recibe retroalimentación inteligente sobre tu código, sugiriendo optimizaciones y mejores prácticas."
  },
  {
    icon: Clock,
    title: "Evaluación Instantánea",
    description: "Obtén resultados en milisegundos con nuestro sistema de juez de alto rendimiento."
  },
  {
    icon: Users,
    title: "Comunidad Global",
    description: "Únete a miles de programadores, comparte soluciones y aprende de los mejores."
  },
  {
    icon: Code2,
    title: "Múltiples Lenguajes",
    description: "Soportamos C++, Python, Java, JavaScript, Go, Rust y más de 15 lenguajes de programación."
  },
  {
    icon: Zap,
    title: "Práctica Diaria",
    description: "Problemas diarios de diferentes niveles para mantener tus habilidades afiladas."
  },
  {
    icon: Shield,
    title: "Sistema Anti-plagio",
    description: "Garantizamos la integridad de las competencias con detección avanzada de plagios."
  },
  {
    icon: LineChart,
    title: "Estadísticas Detalladas",
    description: "Rastrea tu progreso, analiza tu rendimiento y compara con otros usuarios."
  }
]

export function FeaturesSection() {
  return (
    <section className="py-24 relative">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_bottom,_var(--tw-gradient-stops))] from-primary/5 via-transparent to-transparent" />
      
      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
            Todo lo que necesitas para 
            <span className="text-primary"> dominar algoritmos</span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Una plataforma completa diseñada para llevar tus habilidades de programación al siguiente nivel.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, index) => (
            <Card 
              key={index} 
              className="group relative overflow-hidden border-border bg-card hover:border-primary/50 transition-all duration-300"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
              <CardHeader className="relative pb-2">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 border border-primary/30 group-hover:glow-green-sm transition-all">
                  <feature.icon className="h-6 w-6 text-primary" />
                </div>
              </CardHeader>
              <CardContent className="relative">
                <h3 className="text-lg font-semibold text-foreground mb-2 group-hover:text-primary transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-muted-foreground">
                  {feature.description}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  )
}
