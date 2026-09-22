# Arquitectura Frontend — Goslint Judge

> **Audiencia:** Desarrolladores que trabajan en `admin-app` o `student-app`.
> **Alcance:** Organización de código, carga diferida, patrones de acceso a datos y comunicación en tiempo real.

---

## 1. Organización Feature-Based

### ¿Por qué feature-based y no por tipo?

La organización **por tipo** (`components/`, `pages/`, `hooks/`) funciona bien para proyectos pequeños, pero escala mal: al agregar una feature nueva hay que tocar 4 o 5 carpetas distintas, el acoplamiento oculto crece, y en un monorepo multi-app es difícil saber qué pertenece a qué dominio.

La organización **por feature** agrupa todo el código de un dominio en un único directorio autocontenido. Cambiar o eliminar una feature es quirúrgico: una carpeta, no un `grep` por todo el proyecto.

### Estructura de carpetas

```
src/
├─ app/           # Exclusivo de Next.js App Router — thin wrappers (ver §2)
├─ features/      # Un directorio por dominio funcional
│  └─ <feature>/
│     ├─ components/  # React components de la feature
│     ├─ hooks/       # Hooks específicos de la feature
│     ├─ services/    # Adapters de API específicos (ver §4)
│     ├─ types/       # Tipos TypeScript del dominio
│     └─ index.ts     # API pública — lo único que otras features pueden importar
├─ shared/        # Código reutilizable sin lógica de dominio
│  ├─ ui/
│  │  └─ primitives/  # shadcn/ui — componentes base sin opinión de dominio
│  ├─ hooks/          # Hooks genéricos (use-mobile, use-toast, etc.)
│  ├─ utils/          # Utilidades puras (cn, formatters, etc.)
│  └─ types/          # Tipos compartidos entre features
└─ core/          # Infraestructura transversal — no cambia por feature
   ├─ api/            # Patrón Adapter (ver §4)
   ├─ events/         # Event Bus + tipos de eventos (ver §5)
   └─ config/         # Variables de entorno
```

### Regla de importación

Una feature **solo puede importar de**:
- `@/shared/*` — utilidades y UI genérica
- `@/core/*` — infraestructura base
- La API pública (`index.ts`) de **otra** feature — nunca sus archivos internos

```ts
// ✅ Correcto
import { ContestCard } from "@/features/contests"
import { Button } from "@/shared/ui/primitives/button"
import { eventBus } from "@/core/events"

// ❌ Incorrecto — rompe el encapsulamiento de la feature
import { ContestCard } from "@/features/contests/components/contest-card"
```

### Cómo agregar una feature nueva

1. Crea la carpeta: `src/features/<nombre>/`
2. Agrega subdirectorios que necesites: `components/`, `hooks/`, `services/`, `types/`
3. Crea `index.ts` con los exports públicos
4. Crea el thin wrapper en `src/app/<ruta>/page.tsx` (ver §2)

```
src/features/clarifications/
├─ components/
│  └─ clarifications-page.tsx
├─ hooks/
│  └─ use-clarifications.ts
├─ services/
│  └─ clarifications-service.ts
├─ types/
│  └─ clarification.types.ts
└─ index.ts
```

---

## 2. Code Splitting y Lazy Loading

### Cómo funciona en Next.js App Router

Next.js App Router genera automáticamente un chunk JavaScript separado por cada archivo `page.tsx`. No se necesita `React.lazy` manual para las rutas — el framework lo hace.

Arquitectura adoptada: **thin wrappers en `src/app/`**.

Cada `src/app/<ruta>/page.tsx` es un archivo de 3 líneas que re-exporta el componente real desde `src/features/`:

```tsx
// src/app/contests/page.tsx
import { ContestsPage } from "@/features/contests"
export default ContestsPage
```

Esto preserva el routing de Next.js y al mismo tiempo organiza el código por feature.

### Suspense boundaries — `loading.tsx`

Para cada feature con carga de datos async, agrega un `loading.tsx` junto al `page.tsx`:

```tsx
// src/app/contests/loading.tsx
export default function ContestsLoading() {
  return <div className="flex items-center justify-center min-h-screen">
    <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
  </div>
}
```

Next.js usa automáticamente este componente como Suspense boundary mientras carga el chunk de la página.

### Dynamic imports para componentes pesados

Dentro de una página, usa `dynamic()` de Next.js para componentes pesados (editores de código, gráficas, etc.):

```tsx
import dynamic from "next/dynamic"

const CodeEditor = dynamic(
  () => import("@/features/ai-feedback/components/code-editor"),
  { loading: () => <Skeleton className="h-96" />, ssr: false }
)
```

### Verificar los chunks

Después de `pnpm build`, revisa el output de Next.js. Cada ruta debe aparecer como un chunk separado:

```
Route (app)                    Size     First Load JS
┌ ○ /                          3.2 kB   87.4 kB
├ ○ /contests                  4.1 kB   91.2 kB
├ ○ /contests/[id]             8.3 kB   95.4 kB
├ ○ /ai-feedback               12.1 kB  99.2 kB
```

Si ves un bundle único muy grande, revisar que no haya imports circulares ni re-exports masivos desde `shared/`.

---

## 3. Patrón Adapter — Acceso a datos

### El problema que resuelve

Sin el patrón Adapter, los componentes React hacen `fetch()` directamente. Esto crea acoplamiento fuerte entre la UI y la fuente de datos:

- Es difícil testear los componentes sin un backend real
- Cambiar la URL de la API requiere buscar en todo el proyecto
- No hay un lugar único para manejar autenticación, errores o reintentos

### Estructura

```
src/core/api/
├─ types.ts          # IApiAdapter — la interfaz estable que consumen las features
├─ http-client.ts    # HttpApiAdapter — habla con el backend real via fetch
├─ mock-adapter.ts   # MockApiAdapter — devuelve datos estáticos para dev/tests
└─ index.ts          # Re-exports públicos
```

### La interfaz `IApiAdapter`

```ts
// src/core/api/types.ts
export interface IApiAdapter {
  get<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>>
  post<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  put<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  patch<T>(path: string, body: unknown, options?: RequestOptions): Promise<ApiResponse<T>>
  delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>>
}
```

Las features **solo dependen de esta interfaz**, nunca de la clase concreta.

### Cómo implementar un nuevo adapter de feature

Cada feature tiene su propio `services/` donde mapea las llamadas al `IApiAdapter` a funciones de dominio:

```ts
// src/features/contests/services/contests-service.ts
import type { IApiAdapter } from "@/core/api"
import type { Contest } from "../types/contest.types"

export class ContestsService {
  constructor(private api: IApiAdapter) {}

  async getActiveContests(): Promise<Contest[]> {
    const { data, ok, error } = await this.api.get<Contest[]>("/contests?status=active")
    if (!ok) throw new Error(error ?? "Failed to fetch contests")
    return data
  }

  async getContestById(id: string): Promise<Contest> {
    const { data, ok, error } = await this.api.get<Contest>(`/contests/${id}`)
    if (!ok) throw new Error(error ?? "Contest not found")
    return data
  }
}
```

### Intercambiar implementaciones

```ts
// Para producción
const api = new HttpApiAdapter(env.API_BASE_URL)

// Para desarrollo sin backend
const api = new MockApiAdapter()
  .register("GET", "/contests?status=active", () => ({ data: mockContests }))

// La feature no sabe cuál está usando
const service = new ContestsService(api)
```

El control de qué adapter se usa se centraliza en `env.IS_MOCK` (configurado por `NEXT_PUBLIC_USE_MOCK=true`).

---

## 4. Arquitectura Basada en Eventos en Tiempo Real

### El problema que resuelve

En una plataforma de juez en línea, múltiples partes de la UI deben actualizarse simultáneamente cuando ocurre un evento en el servidor (veredicto de envío, cambio en el ranking, nueva aclaración). Sin un mecanismo centralizado, cada componente haría **polling independiente**, generando:

- Múltiples conexiones WebSocket o peticiones HTTP repetidas
- Código de sincronización duplicado en cada feature
- Inconsistencia temporal entre componentes

El **Event Bus** centraliza la recepción de eventos del servidor y los distribuye a las features que están escuchando.

### Estructura

```
src/core/events/
├─ events.ts       # DomainEvents — tipos de todos los eventos y sus payloads
├─ event-bus.ts    # EventBus — bus Pub/Sub en memoria (singleton)
├─ ws-adapter.ts   # WebSocketAdapter — recibe del servidor, publica en el bus
└─ index.ts        # Re-exports públicos
```

### Contrato de eventos

| Nombre del evento          | Cuándo se dispara                          | Payload                            |
|----------------------------|--------------------------------------------|-------------------------------------|
| `SUBMISSION_VERDICT`       | El juez evalúa un envío                   | `SubmissionVerdictPayload`         |
| `RANKING_UPDATED`          | Cambia la tabla de posiciones              | `RankingUpdatePayload`             |
| `CLARIFICATION_POSTED`     | Nueva aclaración publicada por el admin    | `ClarificationPostedPayload`       |
| `CONTEST_STATUS_CHANGED`   | Una maratón comienza, termina o se pausa   | `ContestStatusChangedPayload`      |

Todos los tipos están en [`src/core/events/events.ts`](apps/student-app/src/core/events/events.ts).

### Cómo una feature se suscribe a eventos

```tsx
// src/features/contests/components/leaderboard.tsx
"use client"
import { useEffect, useState } from "react"
import { eventBus } from "@/core/events"
import type { RankingUpdatePayload } from "@/core/events"

export function Leaderboard({ contestId }: { contestId: string }) {
  const [entries, setEntries] = useState<RankingUpdatePayload["entries"]>([])

  useEffect(() => {
    // Suscribirse — el bus devuelve la función de cleanup
    const unsubscribe = eventBus.subscribe("RANKING_UPDATED", (payload) => {
      if (payload.contestId === contestId) {
        setEntries(payload.entries)
      }
    })

    return unsubscribe // React llama esto al desmontar el componente
  }, [contestId])

  return <>{/* renderizar entries */}</>
}
```

### Cómo fluyen los eventos (de extremo a extremo)

```
Backend WebSocket/SSE
        │
        ▼
WebSocketAdapter.handleMessage()
        │  parsea JSON → { event: "RANKING_UPDATED", payload: {...} }
        ▼
eventBus.publish("RANKING_UPDATED", payload)
        │
        ├──► Leaderboard component → re-render con nuevo ranking
        └──► NotificationBell component → muestra badge
```

### Para agregar un nuevo evento

1. En `events.ts`: agrega el payload type e incorpóralo al mapa `DomainEvents`
2. El bus lo soporta automáticamente — no hay cambios en `event-bus.ts`
3. En la feature: llama a `eventBus.subscribe("NUEVO_EVENTO", handler)`

### Simulación en desarrollo (sin backend)

```ts
import { wsAdapter } from "@/core/events"

// En DevTools o en un botón de debug:
wsAdapter.simulateEvent("SUBMISSION_VERDICT", {
  submissionId: "sub-001",
  contestId: "contest-2",
  problemId: "A",
  userId: "user-42",
  verdict: "ACCEPTED",
  executionTimeMs: 12,
  timestamp: new Date().toISOString(),
})
```

---

## 5. Resumen de cambios realizados (Sept 2026)

### `student-app`

| Antes | Después |
|---|---|
| `app/` en raíz — páginas monolíticas | `src/app/` — thin wrappers de 3 líneas |
| `components/` plano por tipo | `src/features/<feature>/components/` |
| `components/ui/` (shadcn) | `src/shared/ui/primitives/` |
| `hooks/` en raíz | `src/shared/hooks/` |
| `lib/utils.ts` | `src/shared/utils/cn.ts` |
| Sin core/ | `src/core/api/` + `src/core/events/` + `src/core/config/` |
| `@/*` → `./` | `@/*` → `./src/` |

**Features identificadas e implementadas:**
- `home` — landing page (HeroSection, FeaturesSection, upcoming contests)
- `auth` — login y registro
- `contests` — listado, detalle con countdown + ranking, historial
- `ai-feedback` — análisis de código con IA

### `admin-app`

| Estado | Descripción |
|---|---|
| Antes | 3 archivos en `app/` — placeholder "en construcción" |
| Después | Estructura `src/` con `features/` (dashboard, problems, marathons, users), `shared/`, `core/` listos para implementar |

### Qué queda pendiente (próxima etapa)

- Implementar las pantallas de `admin-app` (dashboard, gestión de problemas, maratones, usuarios)
- Implementar features pendientes en `student-app`: `submissions`, `ranking` (standalone), `clarifications`, `profile`, `teams`
- Conectar `ContestsService`, `AuthService`, etc. con el backend real via `HttpApiAdapter`
- Conectar `WebSocketAdapter` al endpoint WS del backend
- Extraer `core/api` y `core/events` a `packages/api-client` y `packages/event-bus` cuando haya duplicación real entre las dos apps
- Agregar `loading.tsx` Suspense boundaries por feature
- Configurar `NEXT_PUBLIC_USE_MOCK` en `.env.local` para desarrollo offline
