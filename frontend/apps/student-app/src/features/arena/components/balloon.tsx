import { useId, type CSSProperties } from "react"
import { cn } from "@/shared/utils/cn"
import { balloonPalette } from "../utils/balloon-color"

interface BalloonProps {
  /** Hex del problema (`#RRGGBB`). Si es inválido se usa un tono neutro del tema. */
  color: string | null | undefined
  /** Texto accesible, p. ej. "Problema A resuelto". */
  label: string
  /** Anima la aparición (globo conseguido en vivo). */
  isNew?: boolean
  /** Celebración más grande (equipo del usuario). */
  celebrate?: boolean
  className?: string
}

/** Posiciones del confeti: ángulo y distancia en px, repartidos alrededor del globo. */
const BURST = Array.from({ length: 12 }, (_, i) => {
  const angle = (i / 12) * Math.PI * 2 + 0.3
  const distance = i % 2 === 0 ? 26 : 18
  return { dx: Math.round(Math.cos(angle) * distance), dy: Math.round(Math.sin(angle) * distance) - 10 }
})

/** Globo vectorial: cuerpo ovalado con punta, nudo, cuerda ondulada, sombreado y reflejo. */
export function Balloon({ color, label, isNew = false, celebrate = false, className }: BalloonProps) {
  const uid = useId().replace(/:/g, "")
  const palette = balloonPalette(color)
  // Sin color válido: gris del tema (currentColor) mezclado con blanco/negro.
  const light = palette?.light ?? "color-mix(in oklab, currentColor 65%, white)"
  const base = palette?.base ?? "currentColor"
  const dark = palette?.dark ?? "color-mix(in oklab, currentColor 78%, black)"
  const deep = palette?.deep ?? "color-mix(in oklab, currentColor 55%, black)"
  const burst = celebrate ? BURST : BURST.filter((_, i) => i % 3 !== 0)

  return (
    <span
      role="listitem"
      title={label}
      className={cn(
        "balloon relative inline-block h-8 w-5 shrink-0 text-muted-foreground",
        isNew && "balloon-new",
        isNew && celebrate && "balloon-new--mine",
        className
      )}
      style={{ "--balloon-color": palette?.base ?? "currentColor" } as CSSProperties}
    >
      {isNew && (
        <span className="balloon-burst" aria-hidden="true">
          {burst.map((p, i) => (
            <i key={i} style={{ "--dx": `${p.dx}px`, "--dy": `${p.dy}px`, "--i": i } as CSSProperties} />
          ))}
        </span>
      )}
      <span className="balloon-float">
        <svg viewBox="0 0 40 64" className="balloon-svg h-full w-full overflow-visible" role="img" aria-label={label}>
          <title>{label}</title>
          <defs>
            <radialGradient id={`${uid}-body`} cx="35%" cy="28%" r="80%">
              <stop offset="0%" style={{ stopColor: light }} />
              <stop offset="55%" style={{ stopColor: base }} />
              <stop offset="100%" style={{ stopColor: dark }} />
            </radialGradient>
          </defs>
          <g className="balloon-cord">
            <path
              d="M20 49 C15 53 25 56 20 59 C16 62 22 63 20 64"
              fill="none"
              strokeWidth="1.2"
              strokeLinecap="round"
              style={{ stroke: deep }}
              opacity="0.8"
            />
          </g>
          <path
            d="M20 2 C31 2 38 11 38 22 C38 33 29 41.5 22.2 45 L25 49.5 L15 49.5 L17.8 45 C11 41.5 2 33 2 22 C2 11 9 2 20 2 Z"
            fill={`url(#${uid}-body)`}
            strokeWidth="1"
            strokeLinejoin="round"
            style={{ stroke: deep }}
          />
          <path d="M17.8 45 L25 49.5 L15 49.5 Z" style={{ fill: deep }} opacity="0.55" />
          <path
            d="M31 28 C30 33 27 37 23 40.5"
            fill="none"
            stroke="#000"
            strokeOpacity="0.14"
            strokeWidth="3"
            strokeLinecap="round"
          />
          <ellipse cx="12.5" cy="13" rx="4" ry="7" transform="rotate(28 12.5 13)" fill="#fff" opacity="0.55" />
          <circle cx="9" cy="24" r="1.4" fill="#fff" opacity="0.4" />
        </svg>
      </span>
    </span>
  )
}
