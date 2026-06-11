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
├── backend/                  ← Microservicios Java / Spring Boot
│   ├── ARCHITECTURE.md       ← 📖 Guía técnica del backend
│   ├── build.gradle          ← Configuración raíz de Gradle
│   ├── settings.gradle       ← Registro de todos los sub-proyectos
│   ├── shared/
│   │   ├── common-domain/         ← Entidades y eventos compartidos (sin Spring)
│   │   └── common-infrastructure/ ← Config transversal (excepciones, JWT base)
│   └── services/
│       ├── auth-service/          (puerto 8081)
│       ├── problem-service/       (puerto 8082)
│       ├── submission-service/    (puerto 8083)
│       ├── judge-service/         (puerto 8084)
│       ├── feedback-service/      (puerto 8085)
│       └── contest-service/       (puerto 8086)
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
│   ├── docker/               ← docker-compose.yml (BD, colas, reverse proxy)
│   └── traefik/              ← Configuración del API Gateway
```

---

## Documentación por área

| Área           | Documento                                                        |
|----------------|------------------------------------------------------------------|
| Backend        | [`backend/ARCHITECTURE.md`](./backend/ARCHITECTURE.md)          |
| Frontend       | [`frontend/README.md`](./frontend/README.md)                     |

---

## Stack Tecnológico

### Backend
| Tecnología     | Rol                                                  |
|----------------|------------------------------------------------------|
| Java 17        | Lenguaje de desarrollo                               |
| Spring Boot 3.2| Framework de microservicios                          |
| Gradle 8.x     | Build system multi-proyecto                          |
| PostgreSQL      | Persistencia relacional (una BD por servicio)        |
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
cd backend

# 1. Inicializar el Gradle Wrapper (solo la primera vez, requiere Gradle global)
gradle wrapper --gradle-version 8.9

# 2. Compilar todos los módulos
./gradlew build

# 3. Correr un microservicio específico
./gradlew :services:auth-service:bootRun
./gradlew :services:problem-service:bootRun
```

> ⚠️ Cada microservicio necesita su base de datos PostgreSQL corriendo.  
> Próximamente el `docker-compose.yml` levantará toda la infraestructura con un solo comando.

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
| `auth-service`      | 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `problem-service`   | 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `submission-service`| 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `judge-service`     | 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `feedback-service`  | 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `contest-service`   | 🟡 Esqueleto          | Estructura Clean Architecture lista                |
| `student-app`       | 🟢 UI base lista      | Landing, Login, Register, Contests migrados        |
| `admin-app`         | 🔴 Pendiente          | Placeholder - en construcción                      |
| `docker-compose`    | 🔴 Pendiente          | Infraestructura aún por configurar                 |
| `Gradle Wrapper`    | 🔴 Pendiente          | Requiere Gradle global para inicializar            |

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
| `auth`            | `auth-service`                        |
| `problem`         | `problem-service`                     |
| `submission`      | `submission-service`                  |
| `judge`           | `judge-service`                       |
| `feedback`        | `feedback-service`                    |
| `contest`         | `contest-service`                     |
| `common`          | `shared/common-domain` o `common-infra` |
| `student-app`     | Frontend app de estudiantes           |
| `admin-app`       | Frontend panel administrativo         |
| `infra`           | Docker Compose, Traefik, infraestructura |
| `deps`            | Actualización de dependencias         |

---

#### Ejemplos reales del proyecto

```bash
# Nueva funcionalidad
feat(auth): implementar endpoint de registro de usuario con JWT
feat(judge): agregar soporte de sandboxing para código Python
feat(contest): crear caso de uso para inscripción de equipos
feat(student-app): agregar página de historial de envíos

# Corrección de bugs
fix(judge): corregir límite de memoria en contenedor Docker
fix(auth): resolver error 401 al renovar token expirado
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
feat(auth)!: cambiar esquema de respuesta del endpoint /login

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
