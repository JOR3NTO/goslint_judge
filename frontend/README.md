# Goslint Judge — Frontend

> **Monorepo Frontend** | Next.js 16 · TailwindCSS v4 · shadcn/ui · TypeScript · pnpm Workspaces

Plataforma de programación competitiva e interfaz administrativa con arquitectura modular orientada a features.

---

## Estructura del Monorepo

```text
frontend/
├── package.json              ← Raíz del workspace (scripts globales)
├── pnpm-workspace.yaml       ← Declaración de paquetes del monorepo
├── ARCHITECTURE.md           ← Documentación detallada de la arquitectura
├── .gitignore                ← Ignores globales (node_modules, .next, etc.)
├── brand/                    ← Fuente única del logo e iconos (ver brand/README.md)
├── scripts/sync-brand.cjs    ← `pnpm brand:sync`: copia la marca a las apps
│
└── apps/
    ├── landing-app/          ← Landing pública (Puerto 3002); enlaza a student-app
    ├── student-app/          ← App de estudiantes (Puerto 3000): la única completa
    └── admin-app/            ← App administrativa (Puerto 3001): solo esqueleto
```

`student-app` y `landing-app` siguen una **arquitectura orientada a features (Feature-Based Architecture)** dentro de su directorio `src/` (`admin-app` tiene la misma estructura, con features vacías). Esquema de `student-app`:

```text
src/
├─ app/           # Thin wrappers del Next.js App Router (Rutas y Layouts)
├─ features/      # Módulos por dominio funcional: auth, contests, arena, profile
│  └─ <feature>/
│     ├─ components/
│     ├─ hooks/
│     ├─ services/
│     ├─ types/
│     ├─ utils/   # (solo algunas features)
│     └─ index.ts # API pública de la feature
├─ shared/        # Código transversal agnóstico al negocio
│  ├─ ui/         # Navbar, footer, Logo, DemoModeBadge y primitives/ (shadcn/ui)
│  ├─ hooks/      # Hooks genéricos (use-mobile, use-toast, use-session)
│  └─ utils/      # Utilidades (cn)
└─ core/          # Infraestructura: api/ (adapter HTTP + mock), events/, config/
```
*Para más detalles sobre la arquitectura, la capa HTTP, el tiempo real, el modo demo y las limitaciones conocidas, consulta el [`ARCHITECTURE.md`](./ARCHITECTURE.md).*

---

## Aplicaciones

### `student-app` — Puerto 3000
La aplicación principal que usan los estudiantes:
- **Inicio**: la ruta `/` redirige a la landing pública (`landing-app`, puerto 3002); el logo del navbar y del footer también la enlazan (`NEXT_PUBLIC_LANDING_URL`)
- **Registro** (con institución opcional) y **Login** de cuentas
- **Perfil** (`/profile`, ruta protegida) — datos de la cuenta y avatar generado con [DiceBear](https://www.dicebear.com) (estilo `gaze`, CC0) a partir del `username`. Ver [estado del perfil](#estado-del-perfil-student-app)
- **Maratones** (`/contests`, `/past-contests`) — listado con filtros, anteriores, detalle e inscripción con la contraseña de la maratón
- **Equipos** — de 1 a 3 integrantes con capitán; el capitán invita (combo de usuarios con buscador), renombra y quita integrantes
- **Invitaciones** (`/invitations`) — aceptar o rechazar, con contador en el navbar
- **Arena** (`/contests/[id]/arena`) — pestañas **Problemas** (enunciado, editor de código y envío en C, C++, Java o Python 3), **Aclaraciones**, **Envíos** (historial con diálogo de detalle) y **Ranking** (scoreboard, se refresca cada 30 s en maratones activas)
- Estado de los envíos en tiempo real por WebSocket (`/ws/submissions`) con polling de respaldo cada 3 s

#### Estado del perfil (`student-app`)

| Pieza | Estado | Notas |
|---|---|---|
| Página `/profile` | 🟢 Lista | Cabecera (avatar, nombre, `@username`, rol), detalle de cuenta, skeleton y error con reintento |
| Avatar DiceBear | 🟢 Lista | Semilla = `username` (o `id` si el perfil es parcial). Determinístico, no se guarda en BD. Se renderiza como `<img>` con data URI, sin `dangerouslySetInnerHTML` |
| Navbar con sesión | 🟢 Lista | Con token muestra "Mi perfil"; sin token, Login/Registro |
| Datos completos de la cuenta | 🟡 Depende del backend | El endpoint `GET /api/v1/auth/me` ya existe en `contest-platform`. Contrato en `features/profile/types/profile.types.ts` |
| Fallback sin `/me` | 🟢 Lista | Si responde 404/405 o no hay conexión, se muestra un perfil parcial con los claims del JWT (id, correo, rol) y un aviso |
| Cierre de sesión en el front | 🟢 Lista | Botón "Cerrar sesión" en el navbar (escritorio y móvil). Llama a `POST /api/v1/auth/logout`, borra los tokens de `localStorage` y redirige a `/login`, aunque la llamada falle |

**Enrutamiento del API (`student-app`):** el navegador llama a `/api/v1/*` en su propio origen y `next.config.mjs` lo reenvía por prefijo. Hoy **todos los destinos apuntan por defecto a `http://localhost:8080`** (monolito `contest-platform`). Las variables `CONTEST_SERVICE_URL`, `PROBLEM_SERVICE_URL`, `SUBMISSION_SERVICE_URL` y `AUTH_SERVICE_URL` permiten sobrescribir cada destino, pero con el monolito no hace falta definirlas. El WebSocket de envíos no pasa por los rewrites: se conecta directo a `NEXT_PUBLIC_SUBMISSION_WS_URL` (por defecto `ws://localhost:8080/ws/submissions`).

Cómo probarlo sin backend: `NEXT_PUBLIC_USE_MOCK=true pnpm dev:student`, guardar un `auth_token` de prueba en `localStorage` y abrir `/profile`.

### `admin-app` — Puerto 3001
**Esqueleto, sin funcionalidad todavía.** Hoy solo muestra una página "Panel de Administración — En construcción". Tiene creadas las carpetas `features/` `dashboard`, `marathons`, `problems` y `users` (solo `index.ts` placeholder) y `core/` (api, events, config), listas para implementarse.

### `landing-app` — Puerto 3002
Landing pública de una sola página: hero, funciones, **maratones reales públicas** (consulta `GET /api/v1/contests` del backend sin autenticación) y llamadas a la acción hacia `student-app` (login y registro). Como llama al backend desde el navegador, el backend debe permitir CORS para el origen `http://localhost:3002`.

---

## Cómo ejecutar el proyecto

### Prerrequisitos
- **Node.js** ≥ 20
- **pnpm** ≥ 9 → `npm install -g pnpm`

### 1. Instalar dependencias
```bash
# Ejecutar desde la raíz de la carpeta `frontend/`
pnpm install
```

### 2. Configurar variables de entorno
Asegúrate de configurar las variables necesarias para conectar con el backend. Por defecto, en desarrollo el API base apunta a `localhost`.

Todas las variables tienen valor por defecto, así que con el backend en `http://localhost:8080` no necesitas definir nada. Solo se configuran para cambiar esos valores.

`apps/student-app/.env.local` (plantilla en `.env.local.example`):
```env
# Modo demo sin backend (se ignora en producción)
NEXT_PUBLIC_USE_MOCK=false
# Base de la API: /api usa los rewrites de next.config.mjs hacia http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=/api
# WebSocket de envíos (directo al backend)
NEXT_PUBLIC_SUBMISSION_WS_URL=ws://localhost:8080/ws/submissions
# Landing pública (landing-app). Por defecto http://localhost:3002
NEXT_PUBLIC_LANDING_URL=http://localhost:3002
```

`apps/landing-app/.env.local` (plantilla en `.env.example`):
```env
# URL de student-app (login/registro)
NEXT_PUBLIC_APP_URL=http://localhost:3000
# URL del backend contest-platform
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. Correr en modo de desarrollo (Dev Server)

Puedes ejecutar las aplicaciones desde la raíz usando los scripts predefinidos:

```bash
# Correr landing-app (Puerto 3002)
pnpm dev:landing

# Correr student-app (Puerto 3000)
pnpm dev:student

# Correr admin-app (Puerto 3001)
pnpm dev:admin

# Correr las tres apps a la vez
pnpm dev
```

O si prefieres, entrar al directorio de la app específica:
```bash
cd apps/student-app
pnpm dev
```

### 4. Compilar para producción (Build)
Para verificar que todo compila correctamente:
```bash
# Compilar todo el monorepo
pnpm build

# O compilar aplicaciones individualmente
pnpm --filter @uceva-judge/landing-app build
pnpm --filter @uceva-judge/student-app build
pnpm --filter @uceva-judge/admin-app build
```

`student-app` y `landing-app` tienen `typescript.ignoreBuildErrors: true`, así que el build no falla por errores de tipos. Comprueba los tipos aparte:
```bash
cd apps/student-app
node_modules/.bin/tsc --noEmit   # o: pnpm type-check
```

### 5. Sincronizar la marca
Si cambian los logos de `brand/`, ejecuta `pnpm brand:sync` desde `frontend/` (copia a `public/brand/` de `student-app` y `landing-app`). Ver [`brand/README.md`](./brand/README.md).

---

## Modo demo (mock)

Permite ver y probar **todo el flujo del estudiante sin backend ni base de datos**. Las respuestas
las simula `apps/student-app/src/core/api/mock/` (un servidor en memoria con enrutador) y el estado
se guarda en `sessionStorage` de la pestaña.

### Cómo activarlo

Desde `frontend/`:

```bash
# bash / Git Bash
NEXT_PUBLIC_USE_MOCK=true pnpm dev:student
```

```powershell
# PowerShell
$env:NEXT_PUBLIC_USE_MOCK="true"; pnpm dev:student
```

O crea `apps/student-app/.env.local` a partir de `apps/student-app/.env.local.example` (ya trae `NEXT_PUBLIC_USE_MOCK=true`) (el archivo
`.env.local` no se versiona) y deja `NEXT_PUBLIC_USE_MOCK=true`.

El mock **nunca** se activa en producción: con `next build` / `next start` se ignora, se avisa en
consola y la app usa el backend real. Con `NEXT_PUBLIC_USE_MOCK` distinto de `true` el comportamiento
es el de siempre.

Con el mock activo aparece la insignia **"Demo"** (`DemoModeBadge`) en la barra superior, con
un botón para **reiniciar los datos demo**. En `/login` aparece además **"Entrar como estudiante demo"**.

### Credenciales demo

| Qué | Dato |
|-----|------|
| Estudiante demo | `estudiante@uceva.edu.co` / `Demo1234!` (usuario `demo_student`) |
| Contraseña de la maratón activa y de `Copa Goslint Novatos` (UPCOMING) | `maraton123` |
| Contraseña de `Reto Relámpago de Algoritmos` (UPCOMING) | `reto-secreto-2026` |
| Compañeros ficticios (`ana.gomez@uceva.edu.co`, `carlos.mendoza@uceva.edu.co`, `laura.restrepo@uceva.edu.co`, etc.) | `Demo1234!` |
| Organizador (ADMIN) | `coordinacion@uceva.edu.co` / `Demo1234!` |

### Qué cubre

- **Auth**: login (400 por campo, 401 con contraseña incorrecta, 423 tras 5 intentos fallidos durante 15 min),
  registro con las mismas validaciones que el módulo `auth` del backend (400 y 409 por correo o usuario repetido),
  `me`, `refresh`, `logout` y `check-username`. El token es un **JWT falso** (`header.payload.mock-signature`)
  con expiración de 15 min, así que caduca igual que el real. Un verificador real lo rechaza.
- **Maratones**: cinco maratones sembradas: `Maratón de Programación UCEVA 2026-II` ACTIVE (el estudiante ya está inscrito con «Los Algoritmos»),
  `Copa Goslint Novatos` y `Reto Relámpago de Algoritmos` UPCOMING, `Maratón de Programación UCEVA 2026-I` y `Copa Goslint de Otoño 2026` ENDED. El estado se recalcula según la hora real.
- **Equipos**: inscripción con contraseña (403 si es incorrecta, 409 si el nombre ya existe o el usuario
  ya está en un equipo, 409 si la maratón ya inició), renombrar, quitar integrantes e invitar.
  Las invitaciones solo las envía el capitán, con máximo 3 integrantes y antes del inicio.
- **Invitaciones**: aceptar (y pasar a ser integrante), rechazar, estados `PENDING`, `ACCEPTED`,
  `REJECTED` y `EXPIRED`, y contador en la navbar.
- **Envíos**: `PENDING → QUEUED → JUDGING → JUDGED` en unos 6 s, con el polling de la arena
  (sin WebSocket). Errores: 403 si la maratón no está ACTIVE o quien envía no es integrante, 409 si el
  envío es idéntico a uno previo, 400 con lenguaje no válido.
- **Ranking ICPC**: puntos y penalización calculados desde los envíos juzgados.
- **Búsqueda de usuarios** para invitar (`GET /v1/users/search`).
- **Aclaraciones**: listar y preguntar (hay algunas ya respondidas en los datos sembrados; responder como organizador no está).

Reglas del veredicto simulado (`mock/judge.ts`), en este orden:

| Código enviado | Veredicto |
|----------------|-----------|
| Casi vacío (menos de 20 caracteres sin espacios) o con `TODO` | `WRONG_ANSWER` |
| `while(true)` o `while True` | `TIME_LIMIT_EXCEEDED` |
| Contiene `syntax_error` | `COMPILATION_ERROR` |
| Cualquier otro | `ACCEPTED` |

### Qué no cubre

- **No hay juez real**: no compila ni ejecuta código. El veredicto son las reglas de arriba aplicadas al texto.
- **No hay WebSocket**: con el mock activo el seguimiento de envíos es solo por polling.
- **Sin backend**: no hay base de datos, colas ni correos. Las invitaciones no se envían por email.
- **Datos por pestaña**: `sessionStorage` es por pestaña. Una cuenta registrada en una pestaña no existe en otra.
- **Sin** respuestas de aclaraciones (rol organizador), admin-app ni cambio de contraseña o usuario.
- Los tokens (y las cuentas de la demo) siguen siendo falsos: nunca sirven contra el backend real.

### Reiniciar los datos

- Botón **"Reiniciar datos demo"** de la barra superior (pide confirmación y recarga la página).
- O borra la clave `goslint:mock-db:v1` de `sessionStorage` (cerrar la pestaña también la borra).

### Prueba de humo

Sin navegador ni dependencias nuevas, desde `frontend/apps/student-app`:

```bash
node scripts/mock-smoke.cjs
```

Recorre login, listado, inscripción por contraseña, invitación, envío con veredicto y los errores
principales (34 comprobaciones). Usa el paquete `typescript` que ya está instalado en el proyecto.

## Convenciones de Código y Arquitectura

1. **Feature-Based**: Todo el código de negocio va en `src/features/<nombre>`.
2. **Encapsulamiento**: Una feature **solo** puede importar elementos expuestos en el `index.ts` de otra feature. Nunca accedas directamente a sus carpetas internas.
3. **Páginas como Wrappers**: Los archivos en `src/app/` no deben contener lógica. Son "thin wrappers" que importan y renderizan el componente principal desde `src/features/`.
4. **Patrón Adapter para API**: Evita hacer `fetch` directamente en componentes. Usa los servicios de tu feature, que a su vez utilizan `IApiAdapter` inyectado desde `src/core/api`.
5. **Tiempo real**: hoy solo existe el WebSocket de envíos, encapsulado en el hook `use-submissions` de la feature `arena`. `src/core/events` (Event Bus y `WebSocketAdapter`) está creado pero aún sin uso; ver `ARCHITECTURE.md` §5.

---

## Diseño (Design System)

El sistema de diseño de `student-app` y `landing-app` usa un **tema claro** con acento verde Goslint:

- **Color primario:** `oklch(0.52 0.14 150)` (verde); fondo `oklch(0.99 0.003 240)`; el borde de los inputs (`--input`) es más oscuro que `--border` para que se vean.
- **Tipografía:** Geist Sans / Geist Mono
- **Librería de componentes:** shadcn/ui estilo `new-york`, sobre Tailwind CSS v4
- **Criterio:** inputs con fondo blanco y borde visible; jerarquía de botones (`default` = CTA principal verde, `outline` = acción secundaria, `secondary` gris sin CTA); diálogos responsivos con scroll interno.
- **Tema oscuro:** la paleta verde neón original se conserva en la clase `.dark`, pero no se aplica (la app declara `colorScheme: 'light'`).

Los tokens CSS (oklch) se definen en `:root` y se exponen vía `@theme inline` en Tailwind CSS v4, en `src/app/globals.css` de cada app.

---

## Checklist antes de un PR

- [ ] No hay `console.log()` dejados accidentalmente.
- [ ] La estructura respeta las reglas de importación entre `features/`.
- [ ] Las llamadas a la API se hacen mediante el adaptador (no `fetch` directo en componentes).
- [ ] Los componentes de `shared/ui/primitives/` generados por shadcn no fueron modificados drásticamente para lógicas de negocio.
- [ ] El tipado TypeScript es estricto y pasa el check: `pnpm --filter <app> type-check`.
- [ ] El build de producción pasa sin errores: `pnpm build`.

---

*Documento actualizado: Noviembre 2026*
