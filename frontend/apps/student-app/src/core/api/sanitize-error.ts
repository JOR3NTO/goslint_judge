/**
 * Saneador central de mensajes de error.
 *
 * Todo texto que llega del servidor (o de un proxy intermedio) y que se va a pintar en
 * la UI pasa por aquí. Si contiene algo que no debe ver el usuario —un JWT, una cabecera
 * `Authorization`/`Bearer`, un volcado JSON/HTML crudo o una traza— se sustituye por un
 * mensaje genérico en español. Funciones puras: sin `window` ni dependencias.
 */

/** Mensaje genérico por defecto. */
export const GENERIC_ERROR_MESSAGE = "Ocurrió un error inesperado. Inténtalo de nuevo."

/** Longitud máxima razonable de un mensaje para el usuario. Más largo suele ser un volcado. */
const MAX_MESSAGE_LENGTH = 300

/** JWT compacto: header y payload base64url de objetos JSON (`eyJ...`). */
const JWT_PATTERN = /eyJ[A-Za-z0-9_-]{5,}\.eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]*/
/** Esquema Bearer o cabecera Authorization. */
const BEARER_PATTERN = /\bbearer[\s.]+[A-Za-z0-9._~+/-]{8,}|\bauthorization\s*[:=]/i
/** Nombres de campos/cabeceras de credenciales con un valor a continuación. */
const SECRET_FIELD_PATTERN =
  /\b(access_?token|refresh_?token|auth_token|id_token|password|contrase(ñ|n)a|secret|api[_-]?key|cookie|set-cookie)\b\s*["']?\s*[:=]/i
/**
 * Cadena larga sin espacios parecida a un token o hash (base64url/hex). Sin "/" para no
 * confundir rutas con UUID ("/v1/contests/<uuid>/...") con un secreto.
 */
const LONG_OPAQUE_PATTERN = /[A-Za-z0-9_\-+=]{60,}/
/** Volcados crudos: JSON, HTML o trazas de pila. */
const RAW_DUMP_PATTERN = /^\s*[[{<]|<\/?(html|body|head|pre|div)\b|\bat [\w$.]+\s*\(|Exception\b.*\n|\bHTTP\/\d/i

/** Mensaje genérico según el código HTTP. */
export function genericMessageForStatus(status: number): string {
  if (status === 0) return "No se pudo conectar con el servidor. Verifica tu conexión e inténtalo de nuevo."
  if (status === 400) return "Algunos datos no son válidos. Revísalos e inténtalo de nuevo."
  if (status === 401) return "Tu sesión no es válida. Vuelve a iniciar sesión."
  if (status === 403) return "No tienes permiso para realizar esta acción."
  if (status === 404) return "No se encontró el recurso solicitado."
  if (status === 409) return "La operación entra en conflicto con el estado actual. Revisa los datos."
  if (status === 423) return "La cuenta está bloqueada temporalmente. Inténtalo más tarde."
  if (status === 429) return "Demasiadas solicitudes. Espera un momento e inténtalo de nuevo."
  if (status >= 500) return "El servidor no está disponible en este momento. Inténtalo más tarde."
  return GENERIC_ERROR_MESSAGE
}

/** `true` si el texto contiene credenciales o un volcado que no debe mostrarse. */
export function containsSensitiveData(text: string): boolean {
  return (
    JWT_PATTERN.test(text) ||
    BEARER_PATTERN.test(text) ||
    SECRET_FIELD_PATTERN.test(text) ||
    LONG_OPAQUE_PATTERN.test(text) ||
    RAW_DUMP_PATTERN.test(text)
  )
}

/**
 * Devuelve `raw` si es un mensaje legible y seguro; si no, `fallback`.
 * Acepta `unknown` para poder pasarle directamente un campo de un cuerpo de error.
 */
export function sanitizeErrorMessage(raw: unknown, fallback: string = GENERIC_ERROR_MESSAGE): string {
  if (typeof raw !== "string") return fallback
  const text = raw.trim()
  if (!text || text.length > MAX_MESSAGE_LENGTH) return fallback
  return containsSensitiveData(text) ? fallback : text
}

/**
 * Sanea un cuerpo de error del servidor antes de que lo lea ninguna feature:
 * se queda solo con los campos de primer nivel cuyo valor es un texto seguro
 * (`{ error }`, `{ message }` o `{ campo: "mensaje" }`). El resto se descarta.
 * Un texto inseguro se sustituye por el mensaje genérico del status.
 * Cualquier cosa que no sea un objeto plano devuelve `null`.
 */
export function sanitizeErrorBody(body: unknown, status: number): Record<string, string> | null {
  if (!body || typeof body !== "object" || Array.isArray(body)) return null
  const fallback = genericMessageForStatus(status)
  const clean: Record<string, string> = {}
  for (const [key, value] of Object.entries(body as Record<string, unknown>)) {
    if (typeof value !== "string") continue
    // Un nombre de campo que delate credenciales tampoco se reenvía.
    if (containsSensitiveData(key) || /token|authorization|bearer|cookie|secret/i.test(key)) continue
    clean[key] = sanitizeErrorMessage(value, fallback)
  }
  return Object.keys(clean).length > 0 ? clean : null
}

/** Mensaje para la UI a partir de cualquier error capturado (`catch (err: unknown)`). */
export function errorToUserMessage(err: unknown, fallback: string = GENERIC_ERROR_MESSAGE): string {
  return err instanceof Error ? sanitizeErrorMessage(err.message, fallback) : fallback
}
