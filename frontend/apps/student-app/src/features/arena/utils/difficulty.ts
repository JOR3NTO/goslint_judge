/**
 * Etiqueta de dificultad de un problema.
 *
 * El problem-service guarda `difficult` como entero entre 800 y 3500, múltiplo de 100
 * (validación en `problem-service/.../domain/valueobject/Difficulty.java`), es decir,
 * una escala tipo rating. El dominio no define los cortes Fácil/Media/Difícil, así que
 * se usan estos (SUPUESTO, pendiente de confirmar con el equipo de problemas):
 *   - 800 a 1200  -> Fácil
 *   - 1300 a 1900 -> Media
 *   - 2000 en adelante -> Difícil
 */
export type DifficultyLabel = "Fácil" | "Media" | "Difícil"

export function difficultyLabel(difficult: number | null | undefined): DifficultyLabel | null {
  if (difficult == null || !Number.isFinite(difficult)) return null
  if (difficult <= 1200) return "Fácil"
  if (difficult <= 1900) return "Media"
  return "Difícil"
}

/** Clases de color (tokens del tema) para la etiqueta de dificultad. */
export const DIFFICULTY_TONE: Record<DifficultyLabel, string> = {
  Fácil: "text-primary border-primary/30 bg-primary/10",
  Media: "text-warning border-warning/30 bg-warning/10",
  Difícil: "text-destructive border-destructive/40 bg-destructive/10",
}
