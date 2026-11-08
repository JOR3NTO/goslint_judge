# Goslint Judge

> **Sistema de Evaluación Automática de Código Fuente para Maratones de Programación — UCEVA**  
> Trabajo de Grado · Programa de Ingeniería de Sistemas  
> Unidad Central del Valle del Cauca · Tuluá, Valle del Cauca · 2026

---

## ¿Qué es este proyecto?

El **Goslint Judge** es una plataforma de juez en línea (*Online Judge*) institucional que permite a la UCEVA:

- Gestionar un repositorio propio de problemas algorítmicos.
- Diseñar y ejecutar maratones de programación internas.
- Evaluar código automáticamente en entornos Docker aislados (sandboxing).
- Emitir veredictos oficiales: `Accepted`, `Wrong Answer`, `Time Limit Exceeded`, `Runtime Error`, `Compilation Error`.
- Entregar retroalimentación inteligente mediante un modelo de lenguaje (LLM).

---

## Estructura del Monorepo

```
goslint_judge/
├── .gitignore
├── README.md                 ← Este archivo
│
├── backend/                  ← Java / Spring Boot: monolito modular + juez
│   ├── ARCHITECTURE.md       ← 📖 Guía técnica del backend
│   ├── build.gradle          ← Configuración raíz de Gradle
│   ├── settings.gradle       ← Registro de todos los sub-proyectos
│   ├── shared/
│   │   ├── common-domain/         ← Entidades y eventos compartidos (sin Spring)
│   │   └── common-infrastructure/ ← Config transversal (excepciones, JWT base)
│   └── services/
│       ├── contest-platform/      (puerto 8080) ← monolito modular: auth, problem, contest, submission y feedback (esqueleto)
│       │   └── docs/              ← 📖 AUTH_API.md · RABBITMQ.md · WEBSOCKET.md
│       └── judge-service/         (puerto 8084) ← sandbox bwrap + cgroups v2
│           └── docs/              ← 📖 BWRAP.md · CGROUPS.md · REQUIREMENTS.md · WORKERS.md
│
├── frontend/                 ← Apps Next.js (pnpm Workspaces)
│   ├── README.md             ← 📖 Guía técnica del frontend
│   ├── package.json          ← Scripts globales del workspace
│   ├── pnpm-workspace.yaml
│   └── apps/
│       ├── student-app/  (puerto 3000) ← App pública (estudiantes)
│       └── admin-app/    (puerto 3001) ← Panel administrativo
│
├── infrastructure/           ← Docker Compose, Traefik
│   ├── docker/               ← docker-compose.yml (PostgreSQL, Redis, RabbitMQ y los servicios) + README paso a paso
│   └── traefik/              ← Configuración del API Gateway
│
└── testing/                  ← Utilidades de prueba manual
    ├── ws-judge-simulator/   ← Simula judge-service para ver el WebSocket en vivo
    └── integration-runner/   ← Panel web: flujo completo auth → problema → envío → juez
```

---

## Documentación por área

| Área           | Documento                                                        |
|----------------|------------------------------------------------------------------|
| Backend        | [`backend/ARCHITECTURE.md`](./backend/ARCHITECTURE.md)          |
| Frontend       | [`frontend/README.md`](./frontend/README.md)                     |
| Mensajería (RabbitMQ) | [`backend/services/contest-platform/docs/RABBITMQ.md`](./backend/services/contest-platform/docs/RABBITMQ.md) |
| Notificación en tiempo real (WebSocket) | [`backend/services/contest-platform/docs/WEBSOCKET.md`](./backend/services/contest-platform/docs/WEBSOCKET.md) |
| API de autenticación | [`backend/services/contest-platform/docs/AUTH_API.md`](./backend/services/contest-platform/docs/AUTH_API.md) |
| Sandbox del juez | [`BWRAP.md`](./backend/services/judge-service/docs/BWRAP.md) · [`CGROUPS.md`](./backend/services/judge-service/docs/CGROUPS.md) · [`REQUIREMENTS.md`](./backend/services/judge-service/docs/REQUIREMENTS.md) · [`WORKERS.md`](./backend/services/judge-service/docs/WORKERS.md) |
| Probador manual del WebSocket | [`testing/ws-judge-simulator/README.md`](./testing/ws-judge-simulator/README.md) |

---

## Stack Tecnológico

### Backend
| Tecnología     | Rol                                                  |
|----------------|------------------------------------------------------|
| Java 17        | Lenguaje de desarrollo                               |
| Spring Boot 3.2| Framework (monolito modular + juez)                  |
| Gradle 8.x     | Build system multi-proyecto                          |
| PostgreSQL     | Persistencia relacional (`goslint_judge_db`)         |
| RabbitMQ       | Cola de mensajes (pipeline de evaluación asíncrono)  |
| Redis          | Caché (scoreboard, rate-limiting)                    |
| Docker SDK     | Sandboxing de código fuente del estudiante           |
| Flyway         | Migraciones de esquema de BD                         |
| Traefik        | Reverse proxy / API Gateway                          |
| JWT            | Autenticación stateless                              |

### Frontend
| Tecnología     | Rol                                                  |
|----------------|------------------------------------------------------|
| Next.js 16     | Framework full-stack (App Router)                    |
| React 19       | Librería de UI                                       |
| TypeScript     | Tipado estático                                      |
| TailwindCSS v4 | Estilos utilitarios                                  |
| shadcn/ui      | Componentes accesibles (estilo new-york)             |
| pnpm Workspaces| Gestión del monorepo frontend                        |

---

## Cómo arrancar el proyecto

### Prerrequisitos

| Herramienta | Versión mínima | Instalación                                |
|-------------|----------------|--------------------------------------------|
| Java JDK    | 17             | https://adoptium.net/                      |
| Gradle      | 8.x            | https://gradle.org/install/                |
| Node.js     | 20             | https://nodejs.org/                        |
| pnpm        | 9+             | `npm install -g pnpm`                      |
| Docker      | 24+            | https://www.docker.com/products/docker-desktop |

---

### Backend

```bash
# 1. Levantar la infraestructura (PostgreSQL + Redis + RabbitMQ).
#    Requiere infrastructure/docker/.env (copiar de .env.example). Los servicios de aplicación del
#    compose están desactualizados (ver nota más abajo): levanta solo la infraestructura.
docker compose -f infrastructure/docker/docker-compose.yml up -d postgres redis rabbitmq

cd backend

# 2. Compilar todos los módulos
./gradlew build

# 3. Correr la plataforma (auth, problemas, concursos y envíos) en el puerto 8080
./gradlew :services:contest-platform:bootRun
```

> `contest-platform` **no arranca sin `JWT_SECRET`** (mínimo 32 bytes): la clave
> no tiene valor por defecto a propósito, para que un secreto de ejemplo no acabe
> heredado en un entorno real. Las variables van en `backend/.env`:
>
> ```bash
> set -a && source backend/.env && set +a
> ./gradlew :services:contest-platform:bootRun
> ```
>
> Sin RabbitMQ el servicio arranca igualmente: los envíos quedan en `PENDING` y se
> reintentan solos cuando el broker vuelva.
>
> `judge-service` se levanta aparte (Docker, solo Linux): ver
> [`REQUIREMENTS.md`](./backend/services/judge-service/docs/REQUIREMENTS.md). Debe poder
> llamar a `contest-platform`: `PROBLEM_SERVICE_URL=http://host.docker.internal:8080`.
>
> ⚠️ `infrastructure/docker/docker-compose.yml` y `backend/scripts/start-services.sh` aún
> describen los antiguos `auth/problem/submission/contest-service` (puertos 8081–8086);
> para `contest-platform` usa Gradle o `testing/teams-tester/start.sh`.

---

### Frontend

```bash
cd frontend

# 1. Instalar dependencias (todas las apps del workspace)
pnpm install

# 2. Correr la app de estudiantes (puerto 3000)
pnpm dev:student

# 3. Correr el panel de administración (puerto 3001)
pnpm dev:admin
```

---

## Estado actual del proyecto

| Componente          | Estado                | Notas                                              |
|---------------------|-----------------------|----------------------------------------------------|
| `contest-platform`  | 🟢 Funcional          | Monolito modular en :8080. `auth` (registro, login, refresh, perfil, administración, JWT 15 min / 8 h, bloqueo por intentos en Redis), `problem` (CRUD de problemas y casos de prueba), `contest` (maratones, equipos, invitaciones por correo, aclaraciones, scoreboard) y `submission` (envío con regla de elegibilidad → RabbitMQ → veredicto → WebSocket, con reintentos y DLQ). Esquema con Flyway `V1`–`V11` |
| `judge-service`     | 🟢 Funcional          | Evalúa Python, C, C++ y Java en sandbox (`bwrap` + cgroups v2) con pool de workers. Validado en Manjaro x86_64 y OCI A1-flex ARM |
| módulo `feedback`   | 🔴 Esqueleto          | En `contest-platform`; solo carpetas vacías, sin implementar |
| `student-app`       | 🟢 UI base lista      | Landing, Login, Register, Contests migrados. Perfil (`/profile`) con avatar DiceBear listo; `GET /api/v1/auth/me` ya existe en `contest-platform`, falta conectar el front |
| `admin-app`         | 🔴 Pendiente          | Placeholder - en construcción                      |
| `docker-compose`    | 🟡 Desactualizado     | Infraestructura (PostgreSQL, Redis, RabbitMQ) y perfil `sandbox` OK; los servicios `auth/problem/submission/contest` ya no existen y falta `contest-platform` |
| `traefik`           | 🔴 Pendiente          | `traefik.yml` aún vacío                            |
| `Gradle Wrapper`    | 🟢 Listo              | `./gradlew` disponible en `backend/`               |

### Pendientes principales

1. **Infraestructura de ejecución** — actualizar `docker-compose.yml`, `start-services.sh`/`stop-services.sh` y los `rewrites` de `student-app` para apuntar a `contest-platform` (:8080).
2. **`PROBLEM_SERVICE_URL`** — el juez aún usa `localhost:8082` por defecto (y `APP_PUBLIC_URL` apunta a `:8086`).
3. **Detección de MLE** — en kernels modernos el OOM killer sale con EXIT=0; `TestCaseRunner` debe usar `oom_kill` de `memory.events` en lugar de comparar `memory.peak > memory.max`.
4. **Módulo `feedback`** — retroalimentación con LLM sin implementar.

---

## Convenciones Git

### Ramas

| Rama              | Uso                                                        |
|-------------------|------------------------------------------------------------|
| `main`            | Código estable y probado. Solo merge vía Pull Request.     |
| `develop`         | Base de integración continua. Aquí se une el trabajo diario.|
| `feat/<nombre>`   | Nueva funcionalidad (ej: `feat/auth-login`)                |
| `fix/<nombre>`    | Corrección de bug (ej: `fix/jwt-expiry`)                   |
| `chore/<nombre>`  | Tareas sin impacto funcional (ej: `chore/update-deps`)     |
| `docs/<nombre>`   | Solo cambios de documentación                              |
| `refactor/<nombre>`| Refactorización sin cambio de comportamiento              |

---

### Mensajes de Commit — Conventional Commits

Todos los commits deben seguir el estándar **[Conventional Commits](https://www.conventionalcommits.org/)**:

```
<tipo>(<scope>): <descripción corta en minúsculas>

[cuerpo opcional — qué y por qué, no el cómo]

[footer opcional — refs a issues, breaking changes]
```

---

#### Tipos permitidos

| Tipo       | Cuándo usarlo                                                      |
|------------|--------------------------------------------------------------------|
| `feat`     | Se agrega una nueva funcionalidad al sistema                       |
| `fix`      | Se corrige un bug o comportamiento incorrecto                      |
| `docs`     | Solo cambios en documentación (README, ARCHITECTURE, comentarios)  |
| `style`    | Formato, espaciado, punto y coma — sin cambio de lógica            |
| `refactor` | Reestructuración de código sin agregar funcionalidad ni corregir bug|
| `test`     | Se agregan o modifican pruebas (unitarias, integración)            |
| `chore`    | Tareas de mantenimiento: dependencias, configuración, scripts      |
| `perf`     | Mejoras de rendimiento                                             |
| `ci`       | Cambios en pipelines de CI/CD (GitHub Actions, etc.)               |
| `build`    | Cambios en el sistema de build (Gradle, scripts de compilación)    |

---

#### Scopes del proyecto

Los scopes identifican **qué parte del sistema** fue modificada:

| Scope             | Corresponde a                         |
|-------------------|---------------------------------------|
| `auth`            | módulo `auth` de `contest-platform`   |
| `problem`         | módulo `problem` de `contest-platform` |
| `submission`      | módulo `submission` de `contest-platform` |
| `judge`           | `judge-service`                       |
| `feedback`        | módulo `feedback` de `contest-platform` |
| `contest`         | módulo `contest` de `contest-platform` |
| `common`          | `shared/common-domain` o `common-infra` |
| `student-app`     | Frontend app de estudiantes           |
| `admin-app`       | Frontend panel administrativo         |
| `infra`           | Docker Compose, Traefik, infraestructura |
| `deps`            | Actualización de dependencias         |

---

#### Ejemplos reales del proyecto

```bash
# Nueva funcionalidad
feat(users): implementar endpoint de registro de usuario con JWT
feat(judge): agregar soporte de sandboxing para código Python
feat(contest): crear caso de uso para inscripción de equipos
feat(student-app): agregar página de historial de envíos

# Corrección de bugs
fix(judge): corregir límite de memoria en contenedor Docker
fix(users): resolver error 401 al renovar token expirado
fix(submission): manejar excepción cuando RabbitMQ no está disponible

# Refactorización
refactor(problem): mover lógica de validación al dominio puro
refactor(common): extraer manejo de errores HTTP a GlobalExceptionHandler

# Documentación
docs(backend): documentar convención de paquetes en ARCHITECTURE.md
docs(judge): agregar comentarios Javadoc a EvaluateSubmissionUseCase

# Tests
test(auth): agregar pruebas unitarias a AuthApplicationService
test(judge): cubrir caso de Time Limit Exceeded en RunInSandboxUseCase

# Mantenimiento
chore(deps): actualizar Spring Boot de 3.2.4 a 3.2.5
chore(infra): configurar docker-compose para base de datos local
build(backend): inicializar Gradle Wrapper versión 8.9
ci: configurar GitHub Actions para build automático en develop
```

---

#### Cambios que rompen compatibilidad (Breaking Changes)

Si el commit introduce un cambio que rompe la API existente, agregar `!` al tipo y documentarlo en el footer:

```bash
feat(users)!: cambiar esquema de respuesta del endpoint /login

BREAKING CHANGE: el campo "token" fue renombrado a "accessToken".
Todos los clientes deben actualizar su consumo del endpoint /api/auth/login.
```

---

#### Reglas generales

- La **descripción** va en **minúsculas** y sin punto final.
- Máximo **72 caracteres** en la primera línea.
- Escribir en **infinitivo**: `implementar`, `agregar`, `corregir`, no `implementé`, `agregado`.
- El scope es **obligatorio** en este proyecto para rastrear cambios por servicio.
- Un commit = **una sola cosa**. No mezclar features con fixes.



## Equipo

| Rol                   | Nombre                          |
|-----------------------|---------------------------------|
| Autor / Desarrollador | Jorge Eduardo Cobo Ocampo       |
| Autor / Desarrollador | Andrés David Guevara            |
| Desarrollador         | Juan Estevan Santiago           |
| Desarrollador         | Sebastian Morales Flores        |
| Directora             | Vivian Milen Orejuela Ruiz      |
| Codirector            | Diego Fernando Chicaiza Burbano |

---

*Facultad de Ingeniería · Programa de Ingeniería de Sistemas*  
*Unidad Central del Valle del Cauca · Tuluá · 2026*
