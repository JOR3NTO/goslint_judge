/**
 * Utilidades del modo mock: identificadores, digest de contraseñas, codificación
 * base64url para los JWT falsos y números pseudoaleatorios deterministas.
 *
 * Nada de aquí es criptográficamente seguro. Solo sirve para el modo demo.
 */

/** UUID determinista para los datos semilla. `kind` identifica la entidad y `n` el ordinal. */
export function uid(kind: number, n: number): string {
  return `${kind.toString(16).padStart(8, "0")}-0000-4000-8000-${n.toString(16).padStart(12, "0")}`
}

/**
 * Id nuevo para registros creados en tiempo de ejecución.
 * `crypto.randomUUID` solo existe en contextos seguros (https o localhost); al abrir la
 * app por IP de la red local hace falta el fallback.
 */
export function newId(): string {
  const cryptoApi = (globalThis as { crypto?: { randomUUID?: () => string } }).crypto
  if (cryptoApi && typeof cryptoApi.randomUUID === "function") {
    return cryptoApi.randomUUID()
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = Math.floor(Math.random() * 16)
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16)
  })
}

/** FNV-1a de 32 bits. Determinista y barato. */
export function hash32(text: string): number {
  let h = 0x811c9dc5
  for (let i = 0; i < text.length; i++) {
    h = Math.imul(h ^ text.charCodeAt(i), 0x01000193)
  }
  return h >>> 0
}

/**
 * "Digest" de contraseña del mock. NO es un hash seguro: basta para comparar
 * sin guardar la contraseña en claro dentro del estado.
 */
export function digest(password: string): string {
  return hash32(`goslint-mock:${password}`).toString(16).padStart(8, "0")
}

/** Entero pseudoaleatorio en [min, max] que depende solo de `seed`. */
export function pseudoRandom(seed: string, min: number, max: number): number {
  if (max <= min) return min
  return min + (hash32(seed) % (max - min + 1))
}

/** Codifica texto (UTF-8) en base64url, como exige el formato JWT. */
export function base64UrlEncode(text: string): string {
  const bytes = new TextEncoder().encode(text)
  let binary = ""
  bytes.forEach((b) => {
    binary += String.fromCharCode(b)
  })
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
}

/** Decodifica base64url a texto UTF-8. Lanza si el contenido no es válido. */
export function base64UrlDecode(input: string): string {
  const padded = input.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(input.length / 4) * 4, "=")
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export function isoAt(ms: number): string {
  return new Date(ms).toISOString()
}
