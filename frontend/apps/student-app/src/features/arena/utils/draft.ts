/**
 * Borradores de código por problema + lenguaje, guardados en localStorage.
 * Cada acceso va envuelto en try/catch: en modo privado o con el almacenamiento
 * bloqueado la función devuelve `null` / no hace nada y la UI sigue funcionando.
 */
import type { SubmissionLanguage } from "../types/arena.types"

const KEY_PREFIX = "goslint:arena:draft"

export function draftKey(problemId: string, language: SubmissionLanguage): string {
  return `${KEY_PREFIX}:${problemId}:${language}`
}

export function readDraft(problemId: string, language: SubmissionLanguage): string | null {
  try {
    return localStorage.getItem(draftKey(problemId, language))
  } catch {
    return null
  }
}

export function writeDraft(problemId: string, language: SubmissionLanguage, code: string): void {
  try {
    localStorage.setItem(draftKey(problemId, language), code)
  } catch {
    // Almacenamiento no disponible: el borrador solo vive en memoria.
  }
}
