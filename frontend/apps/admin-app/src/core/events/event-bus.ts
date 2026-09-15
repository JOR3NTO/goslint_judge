/**
 * EventBus — bus de eventos en memoria para comunicación desacoplada entre features.
 *
 * Implementa el patrón Publisher/Subscriber (Pub/Sub) de forma type-safe usando el
 * mapa DomainEvents: publish() y subscribe() solo aceptan nombres de eventos y payloads
 * definidos en events.ts.
 *
 * Uso desde una feature (suscripción):
 *   import { eventBus } from "@/core/events"
 *
 *   useEffect(() => {
 *     const unsub = eventBus.subscribe("RANKING_UPDATED", (payload) => {
 *       // payload tiene tipo RankingUpdatePayload automáticamente
 *       setRanking(payload.entries)
 *     })
 *     return unsub // cleanup al desmontar
 *   }, [])
 *
 * Uso desde el adapter WebSocket/SSE (publicación):
 *   eventBus.publish("SUBMISSION_VERDICT", { submissionId, verdict, ... })
 */
import type { DomainEvents, DomainEventName } from "./events"

type Handler<T> = (payload: T) => void

class EventBusImpl {
  private subscribers = new Map<DomainEventName, Set<Handler<unknown>>>()

  subscribe<K extends DomainEventName>(
    event: K,
    handler: Handler<DomainEvents[K]>
  ): () => void {
    if (!this.subscribers.has(event)) {
      this.subscribers.set(event, new Set())
    }
    this.subscribers.get(event)!.add(handler as Handler<unknown>)

    // Devuelve función de cancelación (cleanup)
    return () => {
      this.subscribers.get(event)?.delete(handler as Handler<unknown>)
    }
  }

  publish<K extends DomainEventName>(event: K, payload: DomainEvents[K]): void {
    this.subscribers.get(event)?.forEach((handler) => {
      try {
        handler(payload)
      } catch (err) {
        console.error(`[EventBus] Error in handler for "${event}":`, err)
      }
    })
  }

  /** Elimina todos los suscriptores de un evento (útil en tests) */
  clear(event?: DomainEventName): void {
    if (event) {
      this.subscribers.delete(event)
    } else {
      this.subscribers.clear()
    }
  }
}

/** Instancia singleton del bus — compartida por toda la app */
export const eventBus = new EventBusImpl()
