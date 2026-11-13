# Goslint Judge

> **Sistema de Evaluación Automática de Código Fuente para Maratones de Programación — UCEVA**  
> Trabajo de Grado · Programa de Ingeniería de Sistemas  
> Unidad Central del Valle del Cauca · Tuluá, Valle del Cauca · 2026

---

## ¿Qué es este proyecto?

El **Goslint Judge** es una plataforma de juez en línea (*Online Judge*) institucional que permite a la UCEVA:

- Gestionar un repositorio propio de problemas algorítmicos.
- Diseñar y ejecutar maratones de programación internas.
- Evaluar código en C, C++, Java y Python automáticamente en un sandbox aislado (`bubblewrap` + cgroups v2, solo Linux).
- Emitir veredictos oficiales: `Accepted`, `Wrong Answer`, `Time Limit Exceeded`, `Memory Limit Exceeded`, `Runtime Error` y `Compilation Error` (más `Judge Error` cuando falla el propio juez).

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
│   ├── .env.example          ← Plantilla de variables para correr con Gradle
│   ├── docker/               ← spring-service.Dockerfile (imagen genérica de servicios Spring)
│   ├── scripts/              ← start-services.sh · stop-services.sh · start-judge.sh
│   ├── shared/
│   │   ├── common-domain/         ← Enums, eventos y value objects compartidos (sin Spring)
│   │   └── common-infrastructure/ ← Validación JWT compartida (JwtTokenValidator, TokenBlacklist)
│   └── services/
│       ├── contest-platform/      (puerto 8080) ← monolito modular: auth, problem, contest, submission y feedback (esqueleto)
│       │   └── docs/              ← 📖 AUTH_API.md · RABBITMQ.md · WEBSOCKET.md
│       └── judge-service/         (puerto 8084) ← sandbox bwrap + cgroups v2 (solo Linux)
│           ├── docker/            ← Dockerfile del juez · entrypoint.sh · gen_seccomp.py
│           └── docs/              ← 📖 BWRAP.md · CGROUPS.md · REQUIREMENTS.md · WORKERS.md
│
├── frontend/                 ← Apps Next.js (pnpm Workspaces)
│   ├── README.md             ← 📖 Guía técnica del frontend
│   ├── ARCHITECTURE.md       ← 📖 Arquitectura feature-based del frontend
│   ├── package.json          ← Scripts globales del workspace
│   ├── pnpm-workspace.yaml
│   ├── brand/                ← Logos y favicons oficiales
│   └── apps/
│       ├── student-app/  (puerto 3000) ← App de estudiantes
│       ├── admin-app/    (puerto 3001) ← Panel administrativo (placeholder)
│       └── landing-app/  (puerto 3002) ← Landing pública
│
├── infrastructure/           ← Docker Compose, systemd, Traefik
│   ├── docker/               ← docker-compose.yml (PostgreSQL, Redis, RabbitMQ y perfil `sandbox` del juez) + README
│   ├── systemd/              ← goslint.slice (rama de cgroups del host para el juez)
│   └── traefik/              ← traefik.yml (vacío: gateway pendiente)
│
└── testing/                  ← Utilidades de prueba manual
    ├── ws-judge-simulator/   ← Simula judge-service para ver el WebSocket en vivo
    ├── integration-runner/   ← Panel web (:4200): flujo completo auth → problema → envío → juez
    └── teams-tester/         ← Panel web (:4300): maratones, equipos, invitaciones y envíos
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
| Frontend (arquitectura) | [`frontend/ARCHITECTURE.md`](./frontend/ARCHITECTURE.md) |
| Docker / infraestructura | [`infrastructure/docker/README.md`](./infrastructure/docker/README.md) |
| Probador manual del WebSocket | [`testing/ws-judge-simulator/README.md`](./testing/ws-judge-simulator/README.md) |
| Probador de equipos, invitaciones y envíos | [`testing/teams-tester/README.md`](./testing/teams-tester/README.md) |

---

## Stack Tecnológico

### Backend
| Tecnología     | Rol                                                  |
|----------------|------------------------------------------------------|
| Java 17        | Lenguaje de desarrollo                               |
| Spring Boot 3.2.4 | Framework (monolito modular + juez)               |
| Gradle 8.5     | Build system multi-proyecto (wrapper incluido)       |
| PostgreSQL     | Persistencia relacional (`goslint_judge_db`)         |
| RabbitMQ       | Cola de mensajes (pipeline de evaluación asíncrono)  |
| Redis          | Módulo `auth`: bloqueo por intentos fallidos, refresh tokens y lista negra de JWT |
| bubblewrap + cgroups v2 | Sandbox de ejecución del código del estudiante (`judge-service`, solo Linux) |
| Flyway         | Migraciones de esquema de BD (`V1`–`V11`)            |
| Spring WebSocket | Notificación en vivo del estado de los envíos      |
| Spring Mail    | Correo de invitaciones a equipos (SMTP)              |
| JWT            | Autenticación stateless (access 15 min · refresh 8 h) |
| Traefik        | API Gateway previsto (configuración pendiente)       |

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
#    Requiere infrastructure/docker/.env (copiar de .env.example, con JWT_SECRET definido).
#    Los servicios de aplicación del compose están desactualizados (ver nota más abajo):
#    levanta solo la infraestructura.
docker compose -f infrastructure/docker/docker-compose.yml up -d postgres redis rabbitmq

cd backend

# 2. Compilar todos los módulos
./gradlew build

# 3. Correr la plataforma (auth, problemas, concursos y envíos) en el puerto 8080
./gradlew :services:contest-platform:bootRun
```

> `contest-platform` **no arranca sin `JWT_SECRET`** (mínimo 32 bytes): la clave
> no tiene valor por defecto a propósito, para que un secreto de ejemplo no acabe
> heredado en un entorno real. Las variables van en `backend/.env` (plantilla en
> `backend/.env.example`; `DB_PASSWORD` y `JWT_SECRET` no tienen valor por defecto; el
> resto de variables están en [`backend/ARCHITECTURE.md`](./backend/ARCHITECTURE.md#configuración-y-variables-de-entorno)):
>
> ```bash
> set -a && source backend/.env && set +a
> ./gradlew :services:contest-platform:bootRun
> ```
>
> Sin RabbitMQ el servicio arranca igualmente: los envíos quedan en `PENDING` y se
> reintentan solos cuando el broker vuelva.
>
> Para crear el super administrador al arrancar, define `SUPER_ADMIN_EMAIL` y
> `SUPER_ADMIN_PASSWORD` (mayúscula, número y carácter especial, mínimo 8).
>
> `judge-service` se levanta aparte (Docker, **solo Linux**; en Windows/macOS el resto del
> sistema funciona y los envíos quedan encolados en RabbitMQ sin quien los evalúe): ver
> [`REQUIREMENTS.md`](./backend/services/judge-service/docs/REQUIREMENTS.md) y
> [`infrastructure/docker/README.md`](./infrastructure/docker/README.md). Debe poder
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

# 3. Correr el panel de administración (puerto 3001, placeholder)
pnpm dev:admin

# 4. Correr la landing pública (puerto 3002)
pnpm dev:landing

# O las tres a la vez
pnpm dev
```

**Modo demo (sin backend):** copiar `frontend/apps/student-app/.env.local.example` a `.env.local`
(trae `NEXT_PUBLIC_USE_MOCK=true`) o ejecutar `NEXT_PUBLIC_USE_MOCK=true pnpm dev:student`. La app
responde con datos simulados en el navegador; se ignora en producción. Detalles y credenciales demo
en [`frontend/README.md`](./frontend/README.md). Con backend real, `student-app` reenvía `/api/v1/*`
a `http://localhost:8080` (`next.config.mjs`).

---

## Estado actual del proyecto

| Componente          | Estado                | Notas                                              |
|---------------------|-----------------------|----------------------------------------------------|
| `contest-platform`  | 🟢 Funcional          | Monolito modular en :8080. `auth` (registro con institución, login, refresh, perfil, administración y búsqueda de usuarios, JWT 15 min / 8 h, bloqueo por intentos en Redis), `problem` (CRUD de problemas y casos de prueba; dificultad 800–3500), `contest` (maratones con contraseña, equipos de 1 a 3 integrantes con capitán, invitaciones por correo, aclaraciones, scoreboard) y `submission` (envío con regla de elegibilidad → RabbitMQ → veredicto → WebSocket, con reintentos y DLQ). Esquema con Flyway `V1`–`V11` |
| `judge-service`     | 🟢 Funcional          | Evalúa Python, C, C++ y Java en sandbox (`bwrap` + cgroups v2) con pool de workers. Solo Linux. Validado en Manjaro x86_64 y OCI A1-flex ARM |
| módulo `feedback`   | 🔴 Esqueleto          | En `contest-platform`; solo carpetas vacías, sin implementar |
| `student-app`       | 🟢 Funcional          | Landing enlazada, login, registro, maratones (inscripción con contraseña, equipos e invitaciones), arena (problemas, envío de código, historial, ranking y aclaraciones), perfil y modo demo con datos simulados |
| `landing-app`       | 🟢 Lista              | Landing pública (:3002) que enlaza a `student-app` |
| `admin-app`         | 🔴 Pendiente          | Placeholder («en construcción»); sin pantallas funcionales |
| `docker-compose`    | 🟡 Desactualizado     | Infraestructura (PostgreSQL, Redis, RabbitMQ) y perfil `sandbox` del juez OK; los servicios `auth/problem/submission/contest` ya no existen y falta `contest-platform` |
| `traefik`           | 🔴 Pendiente          | `traefik.yml` aún vacío                            |
| `Gradle Wrapper`    | 🟢 Listo              | `./gradlew` (Gradle 8.5) disponible en `backend/`  |

### Limitaciones y deuda técnica conocida

Hechos verificados en el código (detalle completo en [`backend/ARCHITECTURE.md`](./backend/ARCHITECTURE.md#11-problemas-conocidos-y-deuda-técnica)):

- El puntaje y la penalización del equipo no se actualizan al aceptar un veredicto: el scoreboard ordena por esos campos, pero ningún flujo los modifica.
- Las consultas de envíos (`/submissions/{id}`, `/team/{id}`, `/history`) y la edición de equipos no comprueban que el recurso pertenezca al usuario autenticado.
- Los enunciados y los casos de ejemplo de los problemas son públicos.
- El secreto JWT tiene un valor por defecto de desarrollo en `jwt.secret` (el servicio exige `JWT_SECRET` para arrancar); el WebSocket acepta cualquier origen (`app.websocket.allowed-origins=*`).
- No existe un endpoint para resolver nombres de usuario a partir de ids.
- La restricción de la tabla `submissions` no admite el veredicto `JUDGE_ERROR`, que el juez sí puede emitir.
- `docker-compose.yml`, `start-services.sh` y `stop-services.sh` siguen describiendo los servicios anteriores al monolito.
- `traefik.yml` está vacío y el módulo `feedback` no está implementado.

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
| `landing-app`     | Frontend landing pública              |
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
