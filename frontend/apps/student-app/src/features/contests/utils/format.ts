/** Formateo de fechas y duraciones de maratones (locale es-CO). */

const dateTimeFormatter = new Intl.DateTimeFormat("es-CO", {
  dateStyle: "medium",
  timeStyle: "short",
})

const dateFormatter = new Intl.DateTimeFormat("es-CO", { dateStyle: "medium" })

/** "20 nov 2026, 10:00 a. m." */
export function formatDateTime(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? "—" : dateTimeFormatter.format(date)
}

/** "20 nov 2026" */
export function formatDate(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? "—" : dateFormatter.format(date)
}

/** Duración en ms -> "2 h 30 min", "45 min" o "1 h". */
export function formatDuration(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return "—"
  const totalMinutes = Math.round(ms / 60_000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours === 0) return `${minutes} min`
  if (minutes === 0) return `${hours} h`
  return `${hours} h ${minutes} min`
}

/** Milisegundos hasta `iso` (negativo si ya pasó). */
export function msUntil(iso: string, now: number): number {
  return new Date(iso).getTime() - now
}

/** Diferencia en ms entre inicio y fin de una maratón (calculada en cliente). */
export function contestDurationMs(startTime: string, endTime: string): number {
  return new Date(endTime).getTime() - new Date(startTime).getTime()
}

/** Convierte ms restantes en partes hh:mm:ss (nunca negativas). */
export function splitCountdown(ms: number): { hours: number; minutes: number; seconds: number } {
  const total = Math.max(0, Math.floor(ms / 1000))
  return {
    hours: Math.floor(total / 3600),
    minutes: Math.floor((total % 3600) / 60),
    seconds: total % 60,
  }
}
