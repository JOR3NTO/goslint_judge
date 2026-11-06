/**
 * Persistencia del estado del mock en sessionStorage.
 *
 * Cada operación va en try/catch: el almacenamiento puede estar bloqueado (modo privado,
 * cookies desactivadas) o lleno. En ese caso el mock sigue funcionando solo en memoria.
 */
import { MOCK_SCHEMA_VERSION, MOCK_STORAGE_KEY, type MockState } from "./model"
import { createSeedState } from "./seed"

export interface StorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

/** sessionStorage de la pestaña actual, o `null` si no existe o está bloqueado. */
export function defaultStorage(): StorageLike | null {
  if (typeof window === "undefined") return null
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

/** Lee el estado guardado; si falta, está corrupto o es de otro esquema, siembra datos nuevos. */
export function loadState(storage: StorageLike | null, now: number): MockState {
  if (storage) {
    try {
      const raw = storage.getItem(MOCK_STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw) as MockState
        if (parsed && parsed.schemaVersion === MOCK_SCHEMA_VERSION) return parsed
      }
    } catch {
      // Almacenamiento no disponible o JSON inválido: se resiembra.
    }
  }
  return createSeedState(now)
}

export function saveState(storage: StorageLike | null, state: MockState): void {
  if (!storage) return
  try {
    storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(state))
  } catch {
    // Cuota llena o almacenamiento bloqueado: el estado queda solo en memoria.
  }
}

export function clearStoredState(storage: StorageLike | null): void {
  if (!storage) return
  try {
    storage.removeItem(MOCK_STORAGE_KEY)
  } catch {
    // Nada que borrar.
  }
}
