import { Card, CardContent, CardHeader } from "@/shared/ui/primitives/card"
import { Code2, ListOrdered, MessagesSquare, Timer, Trophy, Users } from "lucide-react"

/** Funciones que la plataforma tiene hoy. */
const features = [
  {
    icon: Trophy,
    title: "Maratones estilo ICPC",
    description:
      "Compite en maratones con reglas estilo ICPC, con fecha de inicio y de fin definidas por el organizador.",
  },
  {
    icon: Users,
    title: "Equipos de hasta 3 integrantes",
    description:
      "Forma tu equipo e invita a otros usuarios de la plataforma para participar juntos.",
  },
  {
    icon: Code2,
    title: "Envío en C, C++, Java y Python",
    description: "Escribe tu solución en uno de estos cuatro lenguajes y envíala desde la plataforma.",
  },
  {
    icon: Timer,
    title: "Veredicto en tiempo real",
    description:
      "Cada envío muestra su veredicto: Accepted, Wrong Answer, Time Limit Exceeded, Memory Limit Exceeded, Runtime Error o Compilation Error. Tu historial guarda el código, el tiempo y la memoria de cada envío.",
  },
  {
    icon: ListOrdered,
    title: "Ranking de la maratón",
    description:
      "Sigue la tabla de posiciones con los problemas resueltos y la penalización por tiempo de cada equipo.",
  },
  {
    icon: MessagesSquare,
    title: "Aclaraciones con el organizador",
    description:
      "Haz preguntas sobre un problema durante la maratón y recibe la respuesta del organizador.",
  },
]

export function FeaturesSection() {
  return (
    <section id="funciones" className="relative scroll-mt-24 py-24">
      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto mb-16 max-w-2xl text-center">
          <h2 className="mb-4 text-3xl font-bold text-foreground sm:text-4xl">
            Funciones de la <span className="text-primary">plataforma</span>
          </h2>
          <p className="text-lg text-muted-foreground">
            Lo que puedes hacer en Goslint Judge para preparar y participar en maratones de programación.
          </p>
        </div>

        <ul className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {features.map((feature) => (
            <li key={feature.title} className="flex">
              <Card className="group w-full border-border bg-card transition-colors hover:border-primary/50">
                <CardHeader className="pb-2">
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg border border-primary/30 bg-primary/10">
                    <feature.icon className="h-6 w-6 text-primary" aria-hidden="true" />
                  </div>
                </CardHeader>
                <CardContent>
                  <h3 className="mb-2 text-lg font-semibold text-foreground transition-colors group-hover:text-primary">
                    {feature.title}
                  </h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
