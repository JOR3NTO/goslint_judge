/**
 * WebSocketAdapter — stub del adapter de tiempo real (WebSocket / SSE).
 *
 * Su responsabilidad: recibir mensajes del servidor y publicarlos en el eventBus.
 * Las features NO se conectan directamente al WebSocket — solo escuchan el bus.
 *
 * Cuando el backend esté listo, reemplaza el stub por la conexión real:
 *   this.ws = new WebSocket(env.WS_URL)
 *   this.ws.onmessage = (msg) => {
 *     const { event, payload } = JSON.parse(msg.data)
 *     eventBus.publish(event, payload)
 *   }
 */
import { eventBus } from "./event-bus"
import type { DomainEventName, DomainEvents } from "./events"

export class WebSocketAdapter {
  private ws: WebSocket | null = null
  private reconnectDelay = 3000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null

  connect(url: string): void {
    if (typeof window === "undefined") return // SSR guard
    if (this.ws?.readyState === WebSocket.OPEN) return

    console.log(`[WebSocketAdapter] Connecting to ${url}`)
    // TODO: reemplazar por conexión real cuando el backend esté listo
    // this.ws = new WebSocket(url)
    // this.ws.onopen = () => console.log("[WebSocketAdapter] Connected")
    // this.ws.onmessage = (msg) => this.handleMessage(msg)
    // this.ws.onclose = () => this.scheduleReconnect(url)
    // this.ws.onerror = (err) => console.error("[WebSocketAdapter] Error:", err)
  }

  private handleMessage(msg: MessageEvent): void {
    try {
      const { event, payload } = JSON.parse(msg.data) as {
        event: DomainEventName
        payload: DomainEvents[DomainEventName]
      }
      eventBus.publish(event, payload as DomainEvents[typeof event])
    } catch (err) {
      console.error("[WebSocketAdapter] Failed to parse message:", err)
    }
  }

  private scheduleReconnect(url: string): void {
    if (this.reconnectTimer) return
    console.log(`[WebSocketAdapter] Reconnecting in ${this.reconnectDelay}ms...`)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect(url)
    }, this.reconnectDelay)
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.ws?.close()
    this.ws = null
  }

  /**
   * simulateEvent — helper para desarrollo:
   * simula la recepción de un evento del backend sin WebSocket real.
   */
  simulateEvent<K extends DomainEventName>(event: K, payload: DomainEvents[K]): void {
    console.log(`[WebSocketAdapter] Simulating event: ${event}`, payload)
    eventBus.publish(event, payload)
  }
}

/** Instancia singleton del adapter de tiempo real */
export const wsAdapter = new WebSocketAdapter()
