# Arquitectura Frontend — Goslint Judge

> **Audiencia:** Desarrolladores que trabajan en `student-app`, `landing-app` o `admin-app`.
> **Alcance:** Monorepo, organización de código por features, capa HTTP y sesión, tiempo real, modo demo, sistema de diseño, marca y limitaciones conocidas.
> **Estado:** refleja el código de `frontend/` tal como está hoy. `student-app` y `landing-app` están implementadas; `admin-app` es solo un esqueleto.

---

## 0. Monorepo y puertos

El frontend es un monorepo **pnpm** (`pnpm-workspace.yaml` declara `apps/*`). No hay carpeta `packages/`: cada app tiene su propio `core/` y `shared/`.

```
frontend/
├─ package.json            # Scripts globales del workspace
├─ pnpm-workspace.yaml     # packages: apps/*
├─ brand/                  # Fuente única del logo y los iconos (ver §9)
├─ scripts/sync-brand.cjs  # Copia la marca a public/brand de cada app
└─ apps/
   ├─ student-app/   # :3000 — app del estudiante (la única con funcionalidad completa)
   ├─ admin-app/     # :3001 — esqueleto "en construcción"
   └─ landing-app/   # :3002 — landing pública
```

| App | Paquete | Puerto | Estado |
|---|---|---|---|
| `student-app` | `@uceva-judge/student-app` | 3000 | Implementada (auth, maratones, equipos, arena, perfil) |
| `admin-app` | `@uceva-judge/admin-app` | 3001 | Stub: una sola página "En construcción" y carpetas `features/` vacías |
| `landing-app` | `@uceva-judge/landing-app` | 3002 | Implementada (hero, funciones, maratones públicas, CTA) |

### Scripts

Desde `frontend/` (`package.json` raíz):

| Script | Qué hace |
|---|---|
| `pnpm dev` | `pnpm --parallel -r dev`: levanta las tres apps a la vez |
| `pnpm dev:student` / `dev:admin` / `dev:landing` | Levanta una sola app |
| `pnpm build` | `pnpm -r build` (las tres apps) |
| `pnpm build:student` / `build:landing` | Compila una sola app (no existe `build:admin`; usa `pnpm --filter @uceva-judge/admin-app build`) |
| `pnpm brand:sync` | `node scripts/sync-brand.cjs`: copia la marca a las apps (ver §9) |

Cada app define `dev`, `build`, `start` (mismo puerto que `dev`), `lint` y `type-check` (`tsc --noEmit`).

### Variables de entorno

Los valores por defecto están en `src/core/config/env.ts` de cada app.

**`student-app`** (`.env.local`, a partir de `.env.local.example`):

| Variable | Por defecto | Uso |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `/api` | Base del `HttpApiAdapter`. Con `/api` las llamadas pasan por los rewrites de `next.config.mjs` |
| `NEXT_PUBLIC_WS_URL` | `/ws` | Declarada en `env.ts`; hoy solo la usaría el stub `WebSocketAdapter` (ver §5) |
| `NEXT_PUBLIC_SUBMISSION_WS_URL` | `ws://localhost:8080/ws/submissions` | WebSocket directo para el estado de los envíos (los rewrites de Next no proxean WS) |
| `NEXT_PUBLIC_LANDING_URL` | `http://localhost:3002` | Destino de `/` y del logo del navbar/footer |
| `NEXT_PUBLIC_USE_MOCK` | `false` | `true` activa el modo demo (solo fuera de producción, ver §7) |
| `CONTEST_SERVICE_URL`, `PROBLEM_SERVICE_URL`, `SUBMISSION_SERVICE_URL`, `AUTH_SERVICE_URL` | `http://localhost:8080` | Solo del servidor de Next (no son `NEXT_PUBLIC_`): destino de los rewrites |

`env.ts` expone además `IS_MOCK` (la variable tal cual) y `MOCK_ENABLED` (`IS_MOCK` y `NODE_ENV !== "production"`). **La UI debe usar `MOCK_ENABLED`.**

**`landing-app`** (`.env.example`): `NEXT_PUBLIC_APP_URL` (por defecto `http://localhost:3000`, URL de `student-app`) y `NEXT_PUBLIC_API_URL` (por defecto `http://localhost:8080`, URL del backend `contest-platform`).

**`admin-app`**: `env.ts` define `API_BASE_URL` (`http://localhost:8080/api`), `WS_URL`, `IS_MOCK` y `NODE_ENV`, pero la app aún no los usa.

---

## 1. Organización Feature-Based

### ¿Por qué feature-based y no por tipo?

La organización **por tipo** (`components/`, `pages/`, `hooks/`) funciona bien para proyectos pequeños, pero escala mal: al agregar una feature nueva hay que tocar 4 o 5 carpetas distintas, el acoplamiento oculto crece, y en un monorepo multi-app es difícil saber qué pertenece a qué dominio.

La organización **por feature** agrupa todo el código de un dominio en un único directorio autocontenido. Cambiar o eliminar una feature es quirúrgico: una carpeta, no un `grep` por todo el proyecto.

### Estructura de carpetas (`student-app`)

```
src/
├─ app/                 # Next.js App Router — thin wrappers (ver §2)
│  ├─ layout.tsx        # Layout raíz: fuentes Geist, metadata, OG
│  ├─ page.tsx          # "/" → redirect a LANDING_URL
│  ├─ login/            # /login
│  ├─ register/         # /register
│  └─ (protected)/      # Grupo de rutas con guard de sesión (layout.tsx)
│     ├─ contests/            # /contests
│     │  └─ [id]/             # /contests/:id (detalle)
│     │     └─ arena/         # /contests/:id/arena
│     ├─ past-contests/       # /past-contests
│     ├─ invitations/         # /invitations
│     └─ profile/             # /profile (con loading.tsx)
├─ features/            # Un directorio por dominio funcional
│  ├─ auth/             # login, registro, logout
│  ├─ contests/         # maratones, equipos, invitaciones
│  ├─ arena/            # competencia: problemas, envíos, ranking, aclaraciones
│  └─ profile/          # perfil del usuario
│     └─ <feature>/
│        ├─ components/  # React components de la feature
│        ├─ hooks/       # Hooks específicos de la feature
│        ├─ services/    # Adapters de API específicos (ver §3)
│        ├─ types/       # Tipos TypeScript del dominio
│        ├─ utils/       # Utilidades de la feature (solo algunas la tienen)
│        └─ index.ts     # API pública — lo único que otras features pueden importar
├─ shared/              # Código reutilizable sin lógica de dominio
│  ├─ ui/
│  │  ├─ primitives/    # shadcn/ui — componentes base sin opinión de dominio
│  │  ├─ logo.tsx       # Componente Logo (ver §9)
│  │  ├─ navbar.tsx, footer.tsx, stats-card.tsx
│  │  ├─ demo-mode-badge.tsx   # Insignia del modo demo (ver §7)
│  │  └─ theme-provider.tsx
│  ├─ hooks/            # use-mobile, use-toast, use-session
│  └─ utils/            # cn.ts
└─ core/                # Infraestructura transversal — no cambia por feature
   ├─ api/              # Patrón Adapter (ver §3) y mock/ (ver §7)
   ├─ events/           # Event Bus y adapter WebSocket genérico (stub, ver §5)
   └─ config/           # env.ts — variables de entorno
```

Notas sobre el árbol real:

- Las cuatro features existentes son `auth`, `contests`, `arena` y `profile`. No hay feature `home`: la ruta `/` redirige a la landing.
- `shared/` no tiene `types/` en `student-app`.
- `features/<feature>/utils/` existe en `arena`, `contests` y `profile`.
- `loading.tsx` solo existe en `/profile`; el resto de páginas gestionan su estado de carga dentro del componente (skeletons).

### Estructura en `landing-app` y `admin-app`

```
landing-app/src/
├─ app/                    # layout, page (thin wrapper), globals.css, iconos
├─ features/landing/       # components, services, types, index.ts
├─ shared/ui/              # navbar, footer, logo, primitives (badge, button, card, tabs)
├─ shared/utils/cn.ts
└─ core/config/env.ts

admin-app/src/
├─ app/                    # layout, page ("En construcción"), globals.css, iconos
├─ features/               # dashboard, marathons, problems, users: solo index.ts (placeholders)
└─ core/                   # api/ (http-client, mock-adapter, types), events/, config/
```

`admin-app` no tiene `shared/` todavía. `landing-app` no tiene `core/api` ni `core/events`: hace su `fetch` público desde el servicio de la feature (ver §8).

### Regla de importación

Una feature **solo puede importar de**:
- `@/shared/*` — utilidades y UI genérica
- `@/core/*` — infraestructura base
- La API pública (`index.ts`) de **otra** feature — nunca sus archivos internos

El alias `@/*` resuelve a `./src/*` en las tres apps.

```ts
// ✅ Correcto
import { readContestSession } from "@/features/contests"
import { Button } from "@/shared/ui/primitives/button"
import { createHttpApi } from "@/core/api"

// ❌ Incorrecto — rompe el encapsulamiento de la feature
import { readContestSession } from "@/features/contests/utils/session"
```

Ejemplo real: `arena` importa `readContestSession` y el tipo `TeamDTO` desde `@/features/contests`. Cuando hace falta la misma lógica y no está exportada, se duplica a propósito (p. ej. `contests/utils/session.ts` repite la decodificación del JWT de `profile/utils/decode-jwt.ts` para no importar archivos internos).

### Cómo agregar una feature nueva

1. Crea la carpeta: `src/features/<nombre>/`
2. Agrega los subdirectorios que necesites: `components/`, `hooks/`, `services/`, `types/`, `utils/`
3. Crea `index.ts` con los exports públicos (componente de página, tipos, servicio)
4. Crea el thin wrapper en `src/app/<ruta>/page.tsx` (ver §2); si la ruta requiere sesión, dentro de `(protected)/`
5. Si llama al backend: crea un servicio que dependa de `IApiAdapter` y una función `createXxxService()` que use `createHttpApi()` (ver §3), y agrega el prefijo en los rewrites si es un prefijo nuevo

Estructura de ejemplo, tomada de `arena`:

```
src/features/arena/
├─ components/   # arena-page, problems-tab, submissions-tab, ranking-tab, clarifications-tab, code-editor, ...
├─ hooks/        # use-arena, use-submissions, use-scoreboard, use-clarifications, use-problem-detail
├─ services/     # arena-service.ts, clarifications-service.ts
├─ types/        # arena.types.ts
├─ utils/        # difficulty, draft, format, language, verdict
└─ index.ts
```

---

## 2. Rutas, layouts y guard de sesión

### Thin wrappers en `src/app/`

Next.js App Router genera automáticamente un chunk JavaScript por cada `page.tsx`. Cada página de `student-app` es un archivo de pocas líneas que re-exporta el componente real desde `src/features/`:

```tsx
// src/app/login/page.tsx
import { LoginPage } from "@/features/auth"

export default LoginPage
```

### Mapa de rutas (`student-app`)

| Ruta | Acceso | Componente (feature) |
|---|---|---|
| `/` | Pública | Redirige a `env.LANDING_URL` |
| `/login` | Pública | `LoginPage` (auth) |
| `/register` | Pública | `RegisterPage` (auth) |
| `/contests` | Protegida | `ContestsPage` (contests) |
| `/contests/[id]` | Protegida | `ContestDetailPage` (contests) |
| `/contests/[id]/arena` | Protegida | `ContestArenaPage` (arena) |
| `/past-contests` | Protegida | `PastContestsPage` (contests) |
| `/invitations` | Protegida | `InvitationsPage` (contests) |
| `/profile` | Protegida | `ProfilePage` (profile) |

### Layouts y guard de sesión

- `src/app/layout.tsx` (raíz): fuentes Geist Sans/Mono, `metadata` (título, Open Graph y Twitter con `/brand/og-image.png`), `viewport` con `colorScheme: 'light'` y `themeColor: '#ffffff'`, `<html lang="es">`. Solo en `NODE_ENV === 'production'` monta `@vercel/analytics`.
- `src/app/(protected)/layout.tsx`: **guard de sesión en el cliente**. Al montar lee `localStorage["auth_token"]`; si no hay token hace `router.replace("/login")`, y mientras tanto muestra un spinner. Si hay token renderiza los hijos en una columna de altura mínima de pantalla (para que el footer quede al fondo). Solo comprueba que *exista* un token, no que sea válido: la validez la resuelve el backend (un `401` limpia la sesión, ver §3).

Como el token está en `localStorage` y no en cookies, no se puede usar el `middleware` de Next.js para proteger rutas (corre en el servidor y no ve `localStorage`).

### Code splitting

No hace falta `React.lazy` para las rutas: el framework separa un chunk por `page.tsx`. Dentro de una página se puede usar `dynamic()` de Next.js para un componente pesado. Hoy el editor de código de la arena (`code-editor.tsx`) es un `textarea` propio sin dependencias, por lo que no se carga de forma diferida.

Para verificar los chunks, revisa el resumen de rutas que imprime `pnpm build`.

---

## 3. Capa HTTP y patrón Adapter

### El problema que resuelve

Sin el patrón Adapter, los componentes React hacen `fetch()` directamente. Esto acopla la UI a la fuente de datos: es difícil testear sin backend, cambiar la URL obliga a buscar en todo el proyecto y no hay un lugar único para autenticación y errores.

### Estructura

```
src/core/api/
├─ types.ts          # IApiAdapter, RequestOptions, ApiResponse, ApiError
├─ http-client.ts    # HttpApiAdapter + createHttpApi()
├─ mock-adapter.ts   # MockApiAdapter genérico (handlers manuales path+método)
├─ mock/             # Servidor mock del modo demo (ver §7)
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

`ApiResponse<T>` es `{ data, status, ok, error? }`. Las features **solo dependen de esta interfaz**, nunca de la clase concreta.

### `createHttpApi()`

Es la fábrica que usan los servicios de las features:

- Con `NEXT_PUBLIC_USE_MOCK=true` y fuera de producción devuelve el **servidor mock** (singleton `getMockServer()`).
- En cualquier otro caso devuelve `new HttpApiAdapter(env.API_BASE_URL)`. Si el mock está pedido en producción, se ignora y se avisa una vez por consola.

### `HttpApiAdapter`

- Antepone `API_BASE_URL` (por defecto `/api`) a cada ruta; las rutas de los servicios son relativas (`/v1/contests`, etc.).
- Envía `Content-Type: application/json` y, **si hay token**, `Authorization: Bearer <auth_token>` leído de `localStorage`.
- **No manda token** en las rutas públicas de auth: `/v1/auth/login`, `/v1/auth/register`, `/v1/auth/refresh` y `/v1/auth/check-username`.
- **Un `401` con token** (sesión caducada o revocada) borra `auth_token` y `refresh_token` de `localStorage` y redirige a `/login` con `window.location.assign`. Un `401` sin token (p. ej. contraseña incorrecta en el login) no dispara esto.
- Parsea el JSON (si falla, `data` es `null`) y, si la respuesta no es OK, devuelve `{ ok: false, status, error }` con `error` tomado de `data.message` o del `statusText`. No lanza excepciones por HTTP: cada servicio decide cómo convertir el error.

### Servicios por feature

Cada feature tiene su `services/` con una clase que recibe un `IApiAdapter` y una función fábrica que le inyecta `createHttpApi()`. Los servicios lanzan errores tipados (`ContestApiError`, `ArenaApiError`, `AuthApiError`).

| Servicio | Endpoints (relativos a `/api`) |
|---|---|
| `AuthService` (auth) | `POST /v1/auth/login`, `POST /v1/auth/logout`. El registro hace `POST /v1/auth/register` directamente desde `register-page.tsx` con `createHttpApi()` |
| `ContestsService` (contests) | `GET /v1/contests`, `/v1/contests/{id}`, `/v1/contests/{id}/problems`, `/v1/contests/{id}/teams`, `/v1/contests/{id}/scoreboard`; `POST /v1/contests/{id}/teams` (inscripción); `GET /v1/teams/user/{userId}`, `/v1/teams/{id}`; `PUT /v1/teams/{id}`; `DELETE /v1/teams/{id}/members/{userId}`; `POST /v1/teams/{id}/invitations`; `GET /v1/users/search?q=`; `GET /v1/invitations/me`; `POST /v1/invitations/{id}/accept` y `/reject`; `GET /v1/problems/{id}` |
| `ArenaService` (arena) | `GET /v1/problems/test-cases/{problemId}/samples`; `POST /v1/submissions`; `GET /v1/submissions/team/{teamId}`; `GET /v1/submissions/{id}` |
| `ClarificationsService` (arena) | `GET /v1/contests/{id}/clarifications`; `POST /v1/contest-problems/{id}/clarifications` |
| `ProfileService` (profile) | `GET /v1/auth/me` (si falla con 404/405 o sin conexión, degrada a los claims del JWT) |

### Rewrites de `next.config.mjs` (`student-app`)

El navegador siempre llama a `/api/v1/...` en el mismo origen y Next reenvía por prefijo (aplica la primera regla que coincide, por eso van de lo específico a lo general). Hoy **todos los destinos apuntan por defecto a `http://localhost:8080`**, el monolito modular `contest-platform`; cada uno se puede sobrescribir con su variable:

| Prefijo | Variable | Por defecto |
|---|---|---|
| `/api/v1/contests`, `/contests/*`, `/contest-problems/*`, `/teams/*`, `/invitations/*`, `/clarifications/*` | `CONTEST_SERVICE_URL` | `http://localhost:8080` |
| `/api/v1/problems/*` | `PROBLEM_SERVICE_URL` | `http://localhost:8080` |
| `/api/v1/submissions/*` | `SUBMISSION_SERVICE_URL` | `http://localhost:8080` |
| Resto de `/api/v1/*` (auth, users, admin) | `AUTH_SERVICE_URL` | `http://localhost:8080` |

Los comentarios de `next.config.mjs` todavía mencionan los puertos 8086, 8082, 8083 y 8081 de los antiguos servicios; los valores reales por defecto son todos `:8080`. Las cuatro variables se conservan por compatibilidad, pero con el monolito basta con no definirlas.

### Manejo de sesión (JWT en `localStorage`)

- `use-login` guarda `accessToken` en `localStorage["auth_token"]` y `refreshToken` en `localStorage["refresh_token"]`, y redirige a `/contests`.
- `use-logout` llama a `POST /v1/auth/logout` con el access token, borra ambos tokens y redirige a `/login`, aunque la llamada falle.
- `useHasSession()` (`shared/hooks/use-session.ts`) devuelve `null` hasta leer `localStorage` (evita errores de hidratación); el navbar lo usa para mostrar "Mi perfil" o Login/Registro.
- `readContestSession()` (exportado por `contests`) devuelve `{ token, userId }` decodificando el claim `sub` del JWT **sin verificar la firma**; solo sirve para saber quién es el usuario en el cliente. Quien autoriza es el backend.
- Los borradores de código de la arena se guardan en `localStorage` con prefijo `goslint:arena:draft` por problema y lenguaje.

---

## 4. Flujo funcional del estudiante

1. **Registro** (`/register`): nombre, apellido, usuario, correo, contraseña e **institución (opcional)**; si queda vacía no se envía la clave. Los errores de validación del backend se muestran por campo. Al registrar redirige a `/login?registered=true`.
2. **Login** (`/login`): valida en el cliente y llama a `POST /v1/auth/login`. Maneja `400` (errores por campo), `401` (credenciales incorrectas) y `423` (cuenta bloqueada). Con el modo demo activo aparece el botón "Entrar como estudiante demo".
3. **Maratones** (`/contests`): listado con tarjetas de conteo (`ContestStats`), búsqueda y filtro por estado (Todos, En curso, Próximamente, Finalizadas), ordenado por estado. **Anteriores** (`/past-contests`): maratones `ENDED` con búsqueda y paginación de 5 en 5.
4. **Detalle** (`/contests/[id]`): información, cuenta regresiva (`ContestTimer`), número de participantes y problemas, pestañas Problemas y Ranking y, si ya está inscrito, el panel "Mi equipo" (`MyTeamPanel`). El botón principal es "Inscribirse" si el estudiante no tiene equipo, o "Entrar al maratón" / "Ver arena" si ya lo tiene.
5. **Inscripción**: `JoinContestDialog` pide el nombre del equipo (3 a 50 caracteres, tras `trim`) y la **contraseña de la maratón**. La inscripción solo está abierta mientras la maratón está `UPCOMING` y no ha empezado (`isRegistrationOpen`).
6. **Equipos** (`MyTeamPanel`): de **1 a 3 integrantes** (`TEAM_MAX_MEMBERS = 3`), con un **capitán**. El capitán puede invitar, renombrar el equipo y quitar integrantes; los demás pueden abandonarlo (el capitán no puede abandonarlo desde ahí).
7. **Invitaciones**: el capitán elige a quién invitar con un **combo de usuarios con buscador** (`UserCombobox`, busca por username con debounce de 250 ms vía `GET /v1/users/search`). El invitado las ve en `/invitations` (estados `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED`), puede aceptar o rechazar, y el navbar muestra el contador de pendientes.
8. **Arena** (`/contests/[id]/arena`): cuatro pestañas.
   - **Problemas**: lista, enunciado con casos de muestra, editor de código y envío. Lenguajes: C, C++, Java y Python 3, con plantilla inicial por lenguaje. El editor es un `textarea` monoespaciado con números de línea (Tab inserta 4 espacios, altura de 280 a 560 px).
   - **Aclaraciones**: lista pública de preguntas y respuestas y formulario para preguntar sobre un problema.
   - **Envíos**: historial del equipo con veredicto y métricas; al elegir uno se abre un diálogo de detalle (`SubmissionDetailDialog`).
   - **Ranking**: scoreboard de la maratón con el equipo propio resaltado. Si la maratón está `ACTIVE`, **se refresca solo cada 30 s** (`SCOREBOARD_REFRESH_MS`) y hay botón de refresco manual.
   - Solo se puede enviar si la maratón está `ACTIVE` y el usuario tiene equipo.
9. **Perfil** (`/profile`): datos de la cuenta y avatar [DiceBear](https://www.dicebear.com) derivado del `username` (determinístico, se renderiza como `<img>` con data URI). Usa `GET /v1/auth/me` y, si no está disponible, muestra un perfil parcial con los claims del JWT y un aviso.

---

## 5. Tiempo real

### Estado real

- **Envíos (en uso).** La pestaña de envíos de la arena se actualiza desde `features/arena/hooks/use-submissions.ts`, que abre un **WebSocket directo** a `env.SUBMISSION_WS_URL` (`ws://localhost:8080/ws/submissions` por defecto) con los subprotocolos `["goslint-judge", "bearer.<token>"]`. Al recibir un mensaje `SUBMISSION_STATUS_UPDATED` del equipo actualiza el envío local; si no lo conoce, recarga el historial. Reconecta con backoff exponencial (1 s, 2 s, ... hasta 30 s) y tras reconectar recarga el historial. Se cierra al desmontar.
- **Respaldo por polling.** Mientras haya envíos no terminados, consulta `GET /v1/submissions/{id}` cada 3 s hasta que lleguen a un estado terminal. Con el mock activo no se abre WebSocket y el polling hace todo el trabajo.
- **Ranking.** No usa WebSocket: es polling cada 30 s (ver §4).

### Event Bus (`core/events`) — infraestructura aún sin uso

```
src/core/events/
├─ events.ts       # DomainEvents — SUBMISSION_VERDICT, RANKING_UPDATED, CLARIFICATION_POSTED, CONTEST_STATUS_CHANGED
├─ event-bus.ts    # EventBus — Pub/Sub en memoria (singleton `eventBus`)
├─ ws-adapter.ts   # WebSocketAdapter — STUB; la conexión real está comentada (TODO)
└─ index.ts
```

Hoy ninguna feature de `student-app` publica ni se suscribe en el bus, y `wsAdapter.connect()` solo escribe en consola. Lo único que funciona es `eventBus.subscribe/publish` y el helper `wsAdapter.simulateEvent(...)` para pruebas manuales. El contrato de eventos y su payload están en [`src/core/events/events.ts`](apps/student-app/src/core/events/events.ts); la misma carpeta existe, sin uso, en `admin-app`.

Si se decide centralizar el tiempo real en el bus, el flujo previsto es: `WebSocketAdapter.handleMessage()` parsea `{ event, payload }`, llama a `eventBus.publish(...)` y las features se suscriben con `eventBus.subscribe(nombre, handler)` (que devuelve la función de cleanup). Para agregar un evento: añade su payload en `events.ts` y regístralo en el mapa `DomainEvents`; el bus no requiere cambios.

---

## 6. Sistema de diseño

- **Stack**: Tailwind CSS v4 (con `@tailwindcss/postcss` y `tw-animate-css`) y **shadcn/ui** estilo `new-york` (`components.json` apunta los alias a `@/shared/ui/primitives`, `@/shared/utils/cn`, `@/shared/hooks`). Iconos de `lucide-react`. Tipografía Geist Sans y Geist Mono.
- **Tema claro por defecto.** Los tokens se definen en `:root` de `src/app/globals.css` en **oklch** (`--background`, `--foreground`, `--primary` verde `oklch(0.52 0.14 150)`, `--border`, `--input`, `--ring`, además de `--success`, `--warning`, `--error`, `--info`, `--glow`) y se exponen a Tailwind con `@theme inline` (`--color-*`, `--radius-*`).
- **Tema oscuro conservado pero no aplicado.** La paleta oscura (verde neón `oklch(0.75 0.2 145)` sobre fondo negro azulado) vive en `.dark`; solo se activaría añadiendo la clase `dark` al `<html>`. La app declara `colorScheme: 'light'` y no la añade. Existen también las utilidades `glow-green`, `glow-green-sm`, `animate-gradient` y `cursor-blink`.
- **Criterio de componentes base** (`shared/ui/primitives/`):
  - `Input`: fondo blanco explícito y borde visible (`--input`, más oscuro que `--border`), foco con borde y anillo verdes, estados deshabilitado y de error.
  - `Button`: jerarquía por variante. `default` es el CTA principal (verde sólido); `outline` es la acción secundaria (fondo blanco, borde visible, hover verde suave); `secondary` es gris claro y no es para CTA; `ghost`, `link` y `destructive` completan el set. Tamaños `default`, `sm`, `lg` e `icon*`.
  - `Dialog`: responsivo, ancho `calc(100% - 2rem)` en móvil, `sm:max-w-lg` en escritorio, alto máximo `100dvh - 2rem` con scroll interno; el pie apila los botones en móvil.
- Los componentes de `primitives/` los genera shadcn: evita meterles lógica de negocio.

`landing-app` y `admin-app` tienen su propio `globals.css`.

---

## 7. Modo demo (mock)

Permite ver todo el flujo del estudiante **sin backend**. Se activa con `NEXT_PUBLIC_USE_MOCK=true` (por ejemplo copiando `.env.local.example` a `.env.local`). **Nunca se activa en producción** (`next build`/`next start`): `createHttpApi()` lo ignora y avisa en consola.

### Estructura

```
src/core/api/mock/
├─ server.ts           # MockServer (implementa IApiAdapter) y getMockServer()
├─ router.ts           # Enrutador de rutas simuladas
├─ routes-auth.ts, routes-contests.ts, routes-problems.ts, routes-submissions.ts, routes-teams.ts
├─ seed.ts             # Datos iniciales (usuarios, maratones, problemas, equipos, invitaciones, aclaraciones)
├─ model.ts            # Tipos y constantes (credenciales demo, reglas de bloqueo)
├─ domain.ts, judge.ts, session.ts, db.ts, util.ts, index.ts
```

- El estado se guarda en `sessionStorage` de la pestaña, bajo la clave `goslint:mock-db:v1`. Cada pestaña tiene sus propios datos.
- Las features **no** importan el mock: lo obtienen a través de `createHttpApi()`.
- `DemoModeBadge` (en el navbar) muestra la insignia "Demo" con un botón para reiniciar los datos; solo se renderiza si `env.MOCK_ENABLED`.
- En `/login` aparece "Entrar como estudiante demo".
- El token es un JWT falso (`header.payload.mock-signature`) con expiración de 15 min.
- El veredicto simulado (`judge.ts`) se decide por reglas sobre el texto del código; no hay juez real ni WebSocket.

### Credenciales demo

| Qué | Dato |
|---|---|
| Estudiante demo | `estudiante@uceva.edu.co` / `Demo1234!` (usuario `demo_student`) |
| Otros usuarios sembrados y organizador (`coordinacion@uceva.edu.co`, rol ADMIN) | `Demo1234!` |
| Contraseña de `Copa Goslint Novatos` (próxima) y de la maratón activa | `maraton123` |
| Contraseña de `Reto Relámpago de Algoritmos` (próxima) | `reto-secreto-2026` |

La lista completa de qué cubre y qué no cubre el mock, las reglas del veredicto y cómo reiniciar los datos está en [`README.md`](./README.md#modo-demo-mock).

### Prueba de humo

```bash
cd frontend/apps/student-app
node scripts/mock-smoke.cjs
```

`scripts/mock-smoke.cjs` transpila los `.ts` del mock con el paquete `typescript` instalado y recorre login, maratones, inscripción por contraseña, invitaciones, envío con veredicto, errores y la fábrica `createHttpApi` (34 comprobaciones; no requiere navegador ni dependencias nuevas).

---

## 8. `landing-app`

Landing pública en el puerto 3002, de una sola página (`/`), con una feature `landing`:

- **Navbar** (anclas `#funciones` y `#maratones`, y botones a `login`/`register` de `student-app`) y **Footer**.
- **Hero**, **Funciones** (`features-section`), **Maratones** (`contests-preview`) y un **CTA final** ("Crear cuenta" / "Ya tengo cuenta").
- Todos los enlaces a login y registro apuntan a `NEXT_PUBLIC_APP_URL` (`student-app`).
- **Maratones reales públicas**: `getPublicContests()` (`services/public-contests-service.ts`) hace `fetch` a `${NEXT_PUBLIC_API_URL}/api/v1/contests` (sin autenticación, `cache: "no-store"`), traduce el estado (`ACTIVE`, `UPCOMING`, `ENDED`), descarta registros inválidos y ordena por pestaña. Si falla, el error llega a la UI; no se muestran datos de respaldo.
- **CORS**: como la landing (`:3002`) llama al backend directamente desde el navegador (no pasa por rewrites), el backend debe permitir el origen `http://localhost:3002` (o el de despliegue) en `GET /api/v1/contests`. Esta política vive en el backend y no se puede comprobar desde el frontend.

---

## 9. Marca y logo

- `frontend/brand/` es la **fuente única** de logos e iconos (detalle de cada archivo en [`brand/README.md`](./brand/README.md)).
- `pnpm brand:sync` (`scripts/sync-brand.cjs`, solo módulos nativos de Node) copia `logo.png`, `logo-dark.png`, `logo-mark.png`, `logo-mark-dark.png` y `og-image.png` a `apps/student-app/public/brand/` y `apps/landing-app/public/brand/`. No se editan esas copias a mano.
- Los iconos de pestaña (`icon.png`, `apple-icon.png`, `favicon.ico`) viven en `src/app/` de **las tres apps** y se copian a mano desde `brand/` si cambian.
- Componente `Logo` (`shared/ui/logo.tsx` en `student-app` y `landing-app`): `variant="full"` (horizontal, navbar y footer) o `variant="mark"` (símbolo cuadrado, usado en login y registro), con `next/image` y las dimensiones reales de cada PNG. El tamaño lo controla `className`.
- `og-image.png` se usa en `metadata.openGraph` y `metadata.twitter`.
- Las variantes `*-dark` y `logo-vertical.png` existen pero no se usan (la app solo tiene tema claro).

---

## 10. Limitaciones conocidas

Confirmadas en el código:

- **JWT en `localStorage`**: tanto `auth_token` como `refresh_token`. Un XSS podría leerlos. El guard de rutas es solo de cliente y no verifica el token, solo su existencia (hay un `TODO` para migrar a cookie httpOnly).
- **Sin renovación silenciosa de sesión**: se guarda el `refresh_token` y `/v1/auth/refresh` figura como ruta pública, pero ningún código del front lo llama. Al caducar el access token, el primer `401` limpia la sesión y manda a `/login`.
- **`ignoreBuildErrors: true`** en `next.config.mjs` de `student-app` y `landing-app`: `pnpm build` no falla por errores de TypeScript. Verifica con `tsc` (ver §11). `admin-app` lo tiene en `false`.
- **Sin cabeceras de seguridad ni CSP** configuradas en ninguna `next.config.mjs`.
- **WebSocket sin proxy**: `SUBMISSION_WS_URL` apunta directo al backend (los rewrites de Next no proxean WS), por lo que en despliegue hay que configurarla explícitamente.
- **Event Bus sin uso** y `WebSocketAdapter` como stub (ver §5).
- **No hay pruebas automatizadas** de frontend (ni unitarias ni e2e); lo más cercano es `scripts/mock-smoke.cjs`, que cubre el mock.
- `admin-app` no tiene funcionalidad.

---

## 11. Convenciones y cómo ejecutar

### Convenciones

- Archivos en `kebab-case`; componentes en `PascalCase`; hooks `use-xxx.ts`; servicios `xxx-service.ts`; tipos `xxx.types.ts`.
- Cada feature expone su API pública solo en `index.ts` y se importa por ahí desde otras features.
- Las páginas de `src/app/` no tienen lógica.
- Las llamadas HTTP salen de los `services/` de la feature, nunca de `fetch` dentro de componentes (la excepción documentada es `landing-app`, que consulta un endpoint público).
- Textos de la interfaz en español.

### Ejecutar

Desde `frontend/`:

```bash
pnpm install                    # dependencias del monorepo
pnpm dev:student                # http://localhost:3000
pnpm dev:landing                # http://localhost:3002
pnpm dev                        # las tres apps en paralelo

# Modo demo (sin backend), en bash
NEXT_PUBLIC_USE_MOCK=true pnpm dev:student
# En PowerShell
$env:NEXT_PUBLIC_USE_MOCK="true"; pnpm dev:student

node apps/student-app/scripts/mock-smoke.cjs   # prueba de humo del mock
pnpm brand:sync                                # sincroniza la marca
```

Comprobación de tipos (recomendada porque el build ignora errores de TS en `student-app` y `landing-app`):

```bash
cd apps/student-app
node_modules/.bin/tsc --noEmit        # o: pnpm type-check
```

### Pruebas existentes

Solo `apps/student-app/scripts/mock-smoke.cjs` (34 comprobaciones sobre el servidor mock). No hay framework de pruebas configurado.

---

## 12. Pendiente

- Implementar las pantallas de `admin-app` (dashboard, problemas, maratones, usuarios).
- Respuesta de aclaraciones por el organizador (el front de `student-app` solo lista y pregunta).
- Decidir si el Event Bus y `WebSocketAdapter` se conectan al backend o se retiran.
- Renovación del access token con el `refresh_token`.
- Extraer `core/api` y `core/events` a paquetes compartidos si aparece duplicación real entre apps.

---

*Documento actualizado: Noviembre 2026*
