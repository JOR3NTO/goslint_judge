# Goslint Judge — Frontend

> **Monorepo Frontend** | Next.js 16 · TailwindCSS v4 · shadcn/ui · TypeScript · pnpm Workspaces

---

## Estructura del Monorepo

```
frontend/
├── package.json              ← Raíz del workspace (scripts globales)
├── pnpm-workspace.yaml       ← Declaración de paquetes del monorepo
├── .gitignore                ← Ignores globales (node_modules, .next, etc.)
│
└── apps/
    ├── student-app/          ← App pública (estudiantes, maratones, submissions)
    │   ├── app/              ← Next.js App Router (rutas y layouts)
    │   ├── components/       ← Componentes reutilizables + shadcn/ui
    │   ├── hooks/            ← React hooks personalizados
    │   ├── lib/              ← Utilidades (cn, fetchers, etc.)
    │   ├── public/           ← Assets estáticos
    │   └── package.json      ← @Goslint Judge/student-app
    │
    └── admin-app/            ← App administrativa (ADMIN/ORGANIZER)
        ├── app/              ← Next.js App Router (rutas y layouts)
        ├── components/       ← Componentes del panel admin
        ├── hooks/            ← React hooks personalizados
        ├── lib/              ← Utilidades
        └── package.json      ← @Goslint Judge/admin-app
```

---

## Apps

### `student-app` — Puerto 3000
La aplicación principal que usan los estudiantes:
- **Landing page** con presentación de la plataforma
- **Login / Registro** de cuentas
- **Maratones** (`/contests`) — listado, inscripción y participación
- **Envío de código** — editor + selección de lenguaje (C, C++, Java, Python)
- **Historial de envíos** — veredictos y métricas de ejecución
- **Retroalimentación IA** (`/ai-feedback`) — sugerencias del LLM
- **Scoreboard** — ranking en tiempo real

**Stack:** Next.js 16 · React 19 · TailwindCSS v4 · shadcn/ui (estilo `new-york`) · Geist font · Lucide icons

### `admin-app` — Puerto 3001
Panel de administración y gestión:
- **Dashboard** de métricas del sistema
- **Gestión de problemas** — CRUD + carga de casos de prueba
- **Gestión de maratones** — crear, configurar y monitorear concursos
- **Gestión de usuarios** — roles, activación, estadísticas
- **Monitor del Judging Engine** — estado de la cola y veredictos

**Stack:** Idéntico al `student-app`. Puerto 3001 para no colisionar.

---

## Cómo arrancar

### Prerrequisitos
- **Node.js** ≥ 20
- **pnpm** ≥ 9 → `npm install -g pnpm`

### Instalar dependencias
```bash
# Desde la raíz del frontend/
pnpm install
```

### Correr en desarrollo

```bash
# Solo el student-app
pnpm dev:student

# Solo el admin-app
pnpm dev:admin
```

O directamente dentro de cada app:
```bash
cd apps/student-app
pnpm dev
```

### Compilar para producción
```bash
pnpm build
```

---

## Convenciones de Código

### Estructura de carpetas por feature (`student-app`)

```
app/
├── (users)/           ← Route group para páginas públicas
│   ├── login/
│   └── register/
├── (dashboard)/      ← Route group para páginas autenticadas
│   ├── contests/
│   ├── problems/
│   ├── submissions/
│   └── ai-feedback/
├── layout.tsx        ← Root layout (fuentes, providers)
└── page.tsx          ← Landing page
```

### Nomenclatura

| Tipo              | Convención              | Ejemplo                          |
|-------------------|-------------------------|----------------------------------|
| Páginas           | `page.tsx` (kebab-case) | `app/contests/[id]/page.tsx`     |
| Componentes       | PascalCase              | `ContestCard.tsx`                |
| Hooks             | camelCase con `use`     | `useContestData.ts`              |
| Utilidades        | camelCase               | `formatVerdict.ts`               |
| Tipos/interfaces  | PascalCase              | `Contest`, `Submission`          |

### Reglas importantes
1. **No lógica de negocio en componentes** → usa hooks o servicios en `lib/`.
2. **Componentes de UI generados con shadcn** van a `components/ui/`, no se modifican.
3. **Componentes propios del proyecto** van directamente en `components/` (ej. `ContestCard.tsx`).
4. **Llamadas a la API** van en `lib/api/` (por servicio: `auth.ts`, `problems.ts`, etc.).
5. **Variables de entorno** nunca en el código — usar `.env.local`.

### Variables de entorno (`.env.local`)
```env
NEXT_PUBLIC_API_USERS_URL=http://localhost:8081
NEXT_PUBLIC_API_PROBLEMS_URL=http://localhost:8082
NEXT_PUBLIC_API_SUBMISSIONS_URL=http://localhost:8083
NEXT_PUBLIC_API_CONTESTS_URL=http://localhost:8086
NEXT_PUBLIC_API_FEEDBACK_URL=http://localhost:8085
```

---

## Diseño (Design System)

El sistema de diseño sigue el tema **dark neon green** (paleta Goslint):

- **Color primario:** `oklch(0.75 0.2 145)` — verde neón `#00ff88`
- **Fondo:** `oklch(0.1 0.01 240)` — negro azulado
- **Tipografía:** Geist Sans / Geist Mono
- **Librería de componentes:** shadcn/ui estilo `new-york`
- **Efectos:** glow-green, cursor-blink, animate-gradient

Los tokens CSS están definidos en `apps/student-app/app/globals.css` y deben replicarse en `admin-app` para coherencia visual.

---

## Checklist antes de un PR

- [ ] No hay `console.log()` dejados accidentalmente.
- [ ] El componente tiene su tipo correcto (`interface Props {...}`).
- [ ] Las llamadas a la API usan las URLs de `.env.local`, no hardcodeadas.
- [ ] Se añadió `"use client"` solo cuando el componente usa estado o efectos del navegador.
- [ ] Los componentes de `components/ui/` generados por shadcn no fueron modificados directamente.
- [ ] El build pasa sin errores: `pnpm build`.

---

*Documento actualizado: 2026-06-10*
