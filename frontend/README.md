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
│
└── apps/
    ├── student-app/          ← App pública (estudiantes, maratones, envíos)
    └── admin-app/            ← App administrativa (ADMIN/ORGANIZER)
```

Ambas aplicaciones (`student-app` y `admin-app`) comparten la misma **arquitectura orientada a features (Feature-Based Architecture)** dentro de sus respectivos directorios `src/`:

```text
src/
├─ app/           # Thin wrappers del Next.js App Router (Rutas y Layouts)
├─ features/      # Módulos por dominio funcional (home, auth, contests, etc.)
│  └─ <feature>/
│     ├─ components/
│     ├─ hooks/
│     ├─ services/
│     ├─ types/
│     └─ index.ts # API pública de la feature
├─ shared/        # Código transversal agnóstico al negocio
│  ├─ ui/         # Componentes UI reusables (e.g., shadcn/ui)
│  ├─ hooks/      # Hooks genéricos
│  ├─ utils/      # Utilidades (cn, formatters)
│  └─ types/      
└─ core/          # Infraestructura (Api Adapters, Event Bus, Config)
```
*Para más detalles sobre esta arquitectura, el Code Splitting y los Eventos, consulta el [`ARCHITECTURE.md`](./ARCHITECTURE.md).*

---

## Aplicaciones

### `student-app` — Puerto 3000
La aplicación principal que usan los estudiantes:
- **Landing page** con presentación de la plataforma
- **Login / Registro** de cuentas
- **Maratones** (`/contests`) — listado, inscripción y participación
- **Envío de código** — editor + selección de lenguaje
- **Historial de envíos** — veredictos y métricas
- **Retroalimentación IA** (`/ai-feedback`) — sugerencias del LLM
- **Scoreboard** — ranking en tiempo real vía WebSockets

### `admin-app` — Puerto 3001
Panel de administración y gestión:
- **Dashboard** de métricas del sistema
- **Gestión de problemas** — CRUD + casos de prueba
- **Gestión de maratones** — crear, configurar y monitorear
- **Gestión de usuarios** — roles, activación, estadísticas
- **Monitor del Judging Engine**

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

En tu `.env.local`:
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
NEXT_PUBLIC_USE_MOCK=false
```

### 3. Correr en modo de desarrollo (Dev Server)

Puedes ejecutar ambas aplicaciones desde la raíz usando los scripts predefinidos:

```bash
# Correr student-app (Puerto 3000)
pnpm dev:student

# Correr admin-app (Puerto 3001)
pnpm dev:admin
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
pnpm --filter @uceva-judge/student-app build
pnpm --filter @uceva-judge/admin-app build
```

---

## Convenciones de Código y Arquitectura

1. **Feature-Based**: Todo el código de negocio va en `src/features/<nombre>`.
2. **Encapsulamiento**: Una feature **solo** puede importar elementos expuestos en el `index.ts` de otra feature. Nunca accedas directamente a sus carpetas internas.
3. **Páginas como Wrappers**: Los archivos en `src/app/` no deben contener lógica. Son "thin wrappers" que importan y renderizan el componente principal desde `src/features/`.
4. **Patrón Adapter para API**: Evita hacer `fetch` directamente en componentes. Usa los servicios de tu feature, que a su vez utilizan `IApiAdapter` inyectado desde `src/core/api`.
5. **Comunicación en Tiempo Real**: No te suscribas a WebSockets en la UI. Escucha eventos desde el Event Bus global (`src/core/events`).

---

## Diseño (Design System)

El sistema de diseño sigue el tema **dark neon green** (paleta Goslint):

- **Color primario:** `oklch(0.75 0.2 145)` — verde neón `#00ff88`
- **Fondo:** `oklch(0.1 0.01 240)` — negro azulado
- **Tipografía:** Geist Sans / Geist Mono
- **Librería de componentes:** shadcn/ui estilo `new-york`
- **Efectos:** glow-green, cursor-blink, animate-gradient

Los tokens CSS están integrados vía `@theme inline` en Tailwind CSS v4. Se encuentran definidos y estandarizados en los archivos `src/app/globals.css` de ambas aplicaciones.

---

## Checklist antes de un PR

- [ ] No hay `console.log()` dejados accidentalmente.
- [ ] La estructura respeta las reglas de importación entre `features/`.
- [ ] Las llamadas a la API se hacen mediante el adaptador (no `fetch` directo en componentes).
- [ ] Los componentes de `shared/ui/primitives/` generados por shadcn no fueron modificados drásticamente para lógicas de negocio.
- [ ] El tipado TypeScript es estricto y pasa el check: `pnpm --filter <app> type-check`.
- [ ] El build de producción pasa sin errores: `pnpm build`.

---

*Documento actualizado: Septiembre 2026*
