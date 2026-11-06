/** Formateo de fechas, tiempos relativos y letras de problema para la arena (locale es-CO). */

const dateTimeFormatter = new Intl.DateTimeFormat("es-CO", {
  dateStyle: "short",
  timeStyle: "medium",
})

const relativeFormatter = new Intl.RelativeTimeFormat("es", { numeric: "auto" })

/** Fecha y hora absolutas: "6/11/26, 10:32:05 a. m." */
export function formatDateTimeFull(iso: string): string {
  const date = new Date(iso)
  return Number.isNaN(date.getTime()) ? "—" : dateTimeFormatter.format(date)
}

/** Tiempo relativo: "hace 2 min", "hace 1 h", "ayer"… */
export function formatRelativeTime(iso: string, now: number = Date.now()): string {
  const time = new Date(iso).getTime()
  if (Number.isNaN(time)) return "—"
  const diffSeconds = Math.round((time - now) / 1000)
  const abs = Math.abs(diffSeconds)
  if (abs < 60) return relativeFormatter.format(diffSeconds, "second")
  if (abs < 3600) return relativeFormatter.format(Math.round(diffSeconds / 60), "minute")
  if (abs < 86400) return relativeFormatter.format(Math.round(diffSeconds / 3600), "hour")
  return relativeFormatter.format(Math.round(diffSeconds / 86400), "day")
}

/** Letra de un problema según su posición (0 -> A, 1 -> B…). Más allá de Z, usa AA, AB… */
export function problemLetter(position: number): string {
  let n = position
  let letters = ""
  do {
    letters = String.fromCharCode(65 + (n % 26)) + letters
    n = Math.floor(n / 26) - 1
  } while (n >= 0)
  return letters
}

/** Límite de memoria en KB -> "256 MB". */
export function formatMemoryLimit(kb: number): string {
  if (!Number.isFinite(kb)) return "—"
  const mb = kb / 1024
  return Number.isInteger(mb) ? `${mb} MB` : `${mb.toFixed(1)} MB`
}

/** Tiempo de ejecución en ms -> "120 ms", o "—" si no hay medición. */
export function formatMs(ms: number | null | undefined): string {
  return ms == null ? "—" : `${ms} ms`
}

/** Memoria en KB -> "2048 KB", o "—" si no hay medición. */
export function formatKb(kb: number | null | undefined): string {
  return kb == null ? "—" : `${kb} KB`
}
