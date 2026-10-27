import { Avatar, Style } from "@dicebear/core"
import gaze from "@dicebear/styles/gaze.json"

/**
 * Instancia del estilo creada una sola vez y reutilizada entre avatares
 * (recomendado por DiceBear; evita re-procesar la definición en cada render).
 * Estilo "Gaze" — licencia CC0 1.0, no requiere atribución.
 */
const style = new Style(gaze)

const cache = new Map<string, string>()

/**
 * Genera el avatar determinístico de un usuario como `data:image/svg+xml`.
 *
 * La misma semilla produce siempre el mismo avatar, por lo que no hay que
 * guardarlo en base de datos: se deriva del `username` (que es único).
 *
 * Seguridad: se devuelve como data URI para usarse en `<img src>`. Un SVG
 * cargado como imagen NO ejecuta scripts, a diferencia de inyectarlo con
 * `dangerouslySetInnerHTML`, que aquí no se usa. La semilla nunca se
 * interpola en HTML.
 */
export function getAvatarDataUri(seed: string): string {
  const cached = cache.get(seed)
  if (cached) return cached

  const uri = new Avatar(style, { seed }).toDataUri()
  cache.set(seed, uri)
  return uri
}
