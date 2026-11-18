/** Utilidades puras de color para los globos del ranking. */

export interface Rgb {
  r: number
  g: number
  b: number
}

export interface BalloonPalette {
  /** Color base del problema, normalizado a #rrggbb. */
  base: string
  /** Tono claro (zona iluminada del cuerpo). */
  light: string
  /** Tono oscuro (zona en sombra). */
  dark: string
  /** Tono más oscuro, para el contorno, el nudo y la cuerda. */
  deep: string
}

const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i

/** Acepta `#RGB` y `#RRGGBB`; devuelve `null` si no es un hex válido. */
export function parseHexColor(input: string | null | undefined): Rgb | null {
  if (!input) return null
  const value = input.trim()
  if (!HEX_RE.test(value)) return null
  const digits = value.slice(1)
  const full = digits.length === 3 ? digits.replace(/./g, (c) => c + c) : digits
  return {
    r: parseInt(full.slice(0, 2), 16),
    g: parseInt(full.slice(2, 4), 16),
    b: parseInt(full.slice(4, 6), 16),
  }
}

function clampChannel(value: number): number {
  return Math.min(255, Math.max(0, Math.round(value)))
}

export function rgbToHex({ r, g, b }: Rgb): string {
  return `#${[r, g, b].map((c) => clampChannel(c).toString(16).padStart(2, "0")).join("")}`
}

/** Mezcla `color` con `target` en la proporción `amount` (0 = color, 1 = target). */
export function mixColors(color: Rgb, target: Rgb, amount: number): Rgb {
  const t = Math.min(1, Math.max(0, amount))
  return {
    r: clampChannel(color.r + (target.r - color.r) * t),
    g: clampChannel(color.g + (target.g - color.g) * t),
    b: clampChannel(color.b + (target.b - color.b) * t),
  }
}

const WHITE: Rgb = { r: 255, g: 255, b: 255 }
const BLACK: Rgb = { r: 0, g: 0, b: 0 }

/** Aclara un color mezclándolo con blanco. */
export function lighten(color: Rgb, amount: number): Rgb {
  return mixColors(color, WHITE, amount)
}

/** Oscurece un color mezclándolo con negro. */
export function darken(color: Rgb, amount: number): Rgb {
  return mixColors(color, BLACK, amount)
}

/** Paleta del globo derivada del hex de la BD; `null` si el hex es inválido. */
export function balloonPalette(input: string | null | undefined): BalloonPalette | null {
  const rgb = parseHexColor(input)
  if (!rgb) return null
  return {
    base: rgbToHex(rgb),
    light: rgbToHex(lighten(rgb, 0.45)),
    dark: rgbToHex(darken(rgb, 0.22)),
    deep: rgbToHex(darken(rgb, 0.45)),
  }
}
