import Image from "next/image"
import { cn } from "@/shared/utils/cn"

/** Dimensiones reales de los PNG de frontend/brand (sincronizados a public/brand). */
const LOGOS = {
  full: { src: "/brand/logo.png", width: 2919, height: 890 },
  mark: { src: "/brand/logo-mark.png", width: 1024, height: 1024 },
} as const

interface LogoProps {
  /** "full": símbolo + texto GOSLINT (horizontal). "mark": solo el símbolo (cuadrado). */
  variant?: keyof typeof LOGOS
  /** El alto lo controla la clase, p. ej. `h-8 w-auto`. */
  className?: string
  /** Usar en logos visibles al cargar la página (navbar) para que no se retrasen. */
  priority?: boolean
}

/** Logo de la plataforma. Solo tema claro: usa logo.png y logo-mark.png. */
export function Logo({ variant = "full", className, priority = false }: LogoProps) {
  const logo = LOGOS[variant]

  return (
    <Image
      src={logo.src}
      width={logo.width}
      height={logo.height}
      alt="Goslint Judge"
      priority={priority}
      className={cn("w-auto", className)}
    />
  )
}
