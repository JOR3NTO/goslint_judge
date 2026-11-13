# Goslint Judge — Guía de Arquitectura del Backend

> **Trabajo de Grado** | Sistema de Evaluación Automática de Código Fuente para Maratones de Programación – UCEVA  
> **Stack**: Java 17 · Spring Boot 3.2.4 · Gradle (Multi-project) · PostgreSQL · RabbitMQ · Redis · Docker  
> **Patrón**: Monolito modular (`contest-platform`: módulos `auth`, `problem`, `contest` y `submission`) + `judge-service` desacoplado por RabbitMQ. Arquitectura Limpia dentro de cada módulo

---

## Tabla de Contenido

1. [Visión general del sistema](#1-visión-general-del-sistema)
2. [Mapa del monorepo](#2-mapa-del-monorepo)
3. [Arquitectura Limpia — La regla de dependencia](#3-arquitectura-limpia--la-regla-de-dependencia)
4. [Módulos compartidos (shared)](#4-módulos-compartidos-shared)
5. [Microservicios — Responsabilidades y dominio](#5-microservicios--responsabilidades-y-dominio)
   - [Estado de implementación](#estado-de-implementación)
6. [Seguridad y autorización](#6-seguridad-y-autorización)
7. [Canal WebSocket (notificaciones en tiempo real)](#7-canal-websocket-notificaciones-en-tiempo-real)
8. [Flujo de datos principal (envío de código)](#8-flujo-de-datos-principal-envío-de-código)
9. [Convenciones de paquetes Java](#9-convenciones-de-paquetes-java)
10. [Configuración de Gradle](#10-configuración-de-gradle)
11. [Problemas conocidos y deuda técnica](#11-problemas-conocidos-y-deuda-técnica)
12. [Cómo agregar un nuevo microservicio](#12-cómo-agregar-un-nuevo-microservicio)
13. [Checklist antes de hacer un PR](#13-checklist-antes-de-hacer-un-pr)

---

## 1. Visión general del sistema

El **Goslint Judge** es un juez en línea (Online Judge System) institucional diseñado para que la UCEVA gestione de forma autónoma su repositorio de problemas algorítmicos y sus maratones de programación. Los estudiantes envían soluciones en C, C++, Java o Python; el sistema las evalúa en un sandbox aislado con `bubblewrap` + cgroups y emite veredictos oficiales (`ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, etc.).

### Componentes tecnológicos principales

| Tecnología     | Rol                                                             |
|----------------|-----------------------------------------------------------------|
| Spring Boot    | Framework de `contest-platform` y `judge-service`                |
| PostgreSQL     | Persistencia relacional (BD `goslint_judge_db` para `contest-platform`; `judge-service` no tiene BD) |
| RabbitMQ       | Cola de mensajes asíncrona para el pipeline de evaluación       |
| Redis          | Caché: intentos fallidos, bloqueos de cuentas, refresh tokens, blacklist JWT |
| bubblewrap     | Sandbox de ejecución de código (sin Docker)                     |
| API LLM        | Retroalimentación inteligente (pendiente)                       |
| Traefik        | API Gateway / reverse proxy (configuración pendiente)            |
| JWT (JJWT 0.12.5) | Autenticación stateless entre clientes y servicios           |
| Flyway         | Migraciones de esquema de base de datos                         |

---

## 2. Mapa del monorepo

```
backend/
├── build.gradle                  ← BOM Spring Boot 3.2.4, Lombok, JaCoCo, tests comunes
├── settings.gradle               ← Declara 2 módulos shared y 2 servicios
├── gradle.properties             ← Variables globales
├── .env                          ← Variables de entorno locales (no commitear)
│
├── shared/                       ← Librerías internas reutilizables
│   ├── common-domain/            ← Enums y eventos que cruzan más de un servicio
│   │   └── build.gradle          ← Sin dependencias externas; solo Lombok
│   └── common-infrastructure/    ← Seguridad base (JWT) reutilizable por todos los servicios
│       └── build.gradle          ← api: spring-web, spring-security, spring-data-redis, jjwt
│
└── services/
    ├── contest-platform/         ← Puerto 8080. Monolito modular (paquete co.uceva.contests_platform)
    │   ├── auth/                 ←   registro, login, perfil, administración de usuarios
    │   ├── problem/              ←   problemas y casos de prueba
    │   ├── contest/              ←   maratones, equipos, invitaciones, aclaraciones, scoreboard
    │   ├── submission/           ←   envíos, RabbitMQ, WebSocket
    │   ├── feedback/             ←   retroalimentación con LLM (esqueleto: solo carpetas vacías)
    │   └── docs/                 ←   AUTH_API.md · RABBITMQ.md · WEBSOCKET.md
    └── judge-service/            ← Puerto 8084. Sandbox (bwrap + cgroups v2), sin BD
```

> `auth-service`, `problem-service`, `submission-service`, `contest-service` y `feedback-service` **ya no existen como servicios**: se integraron en `contest-platform` (rama `refactor/migrar-la-arquitectura-del-sistema-a-un-monolito-modular`). Los módulos se llaman entre sí por inyección de casos de uso, sin HTTP.

`judge-service` tiene la siguiente forma interna (`contest-platform` repite la misma estructura de capas dentro de cada módulo, bajo `co.uceva.contests_platform.<módulo>`; `auth` usa capas planas):

```
services/<nombre>-service/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/co/uceva/<nombre>/
    │   │   ├── domain/           ← Capa 1: Entidades puras
    │   │   ├── application/      ← Capa 2: Casos de uso y puertos
    │   │   └── infrastructure/   ← Capa 3: Spring, BD, HTTP, mensajería
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/     ← Migraciones Flyway (si aplica)
    └── test/
        └── java/co/uceva/<nombre>/
```

---

## 3. Arquitectura Limpia — La regla de dependencia

Cada microservicio internamente sigue la **Clean Architecture** de Robert C. Martin. La regla fundamental es:

> **Las dependencias solo pueden apuntar hacia adentro.** Las capas externas conocen a las internas; las internas nunca conocen a las externas.

```
┌─────────────────────────────────────────────────┐
│                 infrastructure/                  │  ← Spring @Controller, JPA @Repository, RabbitMQ
│  ┌───────────────────────────────────────────┐  │
│  │              application/                 │  │  ← @Service, Casos de Uso, Puertos (interfaces)
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │              domain/                │  │  │  ← Entidades, Value Objects, Excepciones de dominio
│  │  │   (CERO dependencias externas)      │  │  │     Sin anotaciones de Spring ni JPA aquí
│  │  └─────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
         Las flechas de dependencia van → hacia adentro
```

### ¿Qué va en cada capa?

#### `domain/` — El núcleo (lo más importante)
- Clases Java puras: entidades de negocio, value objects, enums de dominio.
- **Cero** imports de `org.springframework.*`, `jakarta.persistence.*`, `com.fasterxml.*`.
- Aquí viven las reglas de negocio que nunca cambian aunque cambie la BD o el framework.
- Ejemplo: `Submission.java`, `Problem.java`, `JudgeTask.java`.

#### `application/` — Casos de uso
- Interfaces (puertos): `SubmissionRepository`, `JudgeResultPublisher`.
- Implementaciones de los casos de uso: `SubmitCodeUseCaseImpl`.
- Aquí se orquesta la lógica de negocio. Puede importar clases de `domain/`.
- **No** importa nada de `infrastructure/` ni de Spring directamente.

#### `infrastructure/` — Los detalles técnicos
- Controladores REST: `@RestController`, `@RequestMapping`.
- Implementaciones de repositorios: `@Repository`, JPA entities, Spring Data.
- Configuración de Spring: `@Configuration`, `@Bean`, `SecurityConfig`.
- Adaptadores de mensajería: `@RabbitListener`, `RabbitTemplate`.
- Aquí sí se usan todas las anotaciones de Spring Boot.

---

## 4. Módulos compartidos (shared)

### `shared/common-domain`
Contiene elementos del dominio que son transversales a más de un microservicio.

**Restricción:** No puede tener dependencias de Spring Boot ni de ningún framework externo. Solo Lombok.

| Clase | Tipo | Descripción |
|-------|------|-------------|
| `ProgrammingLanguage` | Enum | `C`, `CPP`, `JAVA`, `PYTHON` |
| `SubmissionStatus` | Enum | Ciclo de vida: `PENDING` → `QUEUED` → `JUDGING` → `JUDGED`. Terminal de error: `ENQUEUE_ERROR` |
| `VerdictStatus` | Enum | `PENDING`, `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `MEMORY_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR`, `JUDGE_ERROR` |
| `SubmissionReceivedEvent` | Record | Publicado por el módulo `submission` de `contest-platform` al encolar un envío. Contiene: `submissionId`, `teamId`, `problemId`, `language`, `sourceCode`, `submittedAt` |
| `SubmissionJudgingStartedEvent` | Record | Publicado por `judge-service` al tomar el envío. Contiene: `submissionId`, `startedAt` |
| `SubmissionJudgedEvent` | Record | Publicado por `judge-service` al terminar. Contiene: `submissionId`, `verdict`, `executionTimeMs`, `memoryUsedKb`, `judgedAt` |
| `SourceCode` | Value Object | `record SourceCode(String content, ProgrammingLanguage language)`. Valida: contenido no vacío, lenguaje no nulo, tamaño ≤ 128 KB (131 072 bytes). Expone `sizeInBytes()`. |

> **Relación `status` ↔ `verdict`:** Son ortogonales. Mientras el juez trabaja, un envío puede estar `status=QUEUED, verdict=PENDING`. `status` describe *dónde está* el envío en el flujo; `verdict` describe *qué resultado obtuvo*.

### `shared/common-infrastructure`
Contiene la pieza de seguridad JWT compartida por todos los servicios que validan tokens.

**Depende de (vía `api`):** `common-domain` + `spring-boot-starter-web` + `spring-boot-starter-security` + `spring-boot-starter-data-redis` + `jjwt 0.12.5`

| Clase | Descripción |
|-------|-------------|
| `JwtTokenValidator` | Verifica firma HMAC-SHA256, emisor y expiración de un JWT. Constructor `(secret, issuer)`. No emite tokens. Sin anotaciones Spring. |
| `AuthenticatedUser` | Record: identidad extraída de un token ya validado. Campos: `UUID userId`, `String role`. |
| `InvalidTokenException` | `RuntimeException` lanzada cuando el token no supera la validación (firma, expiración, claims faltantes). |

> **Nota:** La dependencia `spring-boot-starter-data-redis` en `common-infrastructure` tiene como objetivo futuro soportar la lista negra de tokens. Actualmente no hay ninguna clase en este módulo que la use.

---

## 5. Microservicios — Responsabilidades y dominio

### Estado de implementación

| Servicio / módulo | Puerto | Estado | Qué hay hoy |
|-------------------|--------|--------|-------------|
| `contest-platform` · módulo `auth` | 8080 | 🟢 Funcional | Registro, login, logout, refresh con rotación, perfil (`/me`, cambio de username y de contraseña), administración de usuarios y super administrador inicial. Emite JWT (Access 15 min y Refresh 8 h, distinguidos por el claim `type`). Bloqueo de cuenta por intentos fallidos y lista negra de tokens en Redis. Tabla `users` creada por Flyway `V7` |
| `contest-platform` · módulo `problem` | 8080 | 🟢 Funcional | CRUD de problemas y casos de prueba, restricciones por rol con `@PreAuthorize`, endpoint público de *samples*. Tablas por Flyway `V11` |
| `contest-platform` · módulo `contest` | 8080 | 🟢 Funcional | Maratones, problemas de maratón (puntos, penalización, globo), equipos con capitán, invitaciones por correo (SMTP), aclaraciones y scoreboard. Flyway `V1`–`V6` |
| `contest-platform` · módulo `submission` | 8080 | 🟢 Funcional | Recepción del envío con regla de elegibilidad (integrante del equipo, concurso activo, problema del concurso), encolamiento en RabbitMQ con confirmación del broker, consumo del veredicto, cierre por error del sistema desde las DLQ, reintento de pendientes y notificación en tiempo real por WebSocket. Flyway `V8`–`V10` |
| `judge-service` | 8084 | 🟢 Funcional | Consume `submission.evaluate`, obtiene casos de prueba y límites del módulo `problem` por HTTP, ejecuta en el sandbox (`bwrap`/cgroups) con un pool de workers (`JUDGE_WORKERS`) y publica el veredicto en `submission.judged`. Evalúa **Python, C, C++ y Java**. El JWT `SERVICE` hacia `contest-platform` lo firma el propio juez con el secreto compartido. Validado end-to-end en Manjaro x86_64 y en Ubuntu OCI A1-flex aarch64. Se despliega en su propia imagen con `bubblewrap` y seccomp (perfil `sandbox` del compose) |
| `contest-platform` · módulo `feedback` | 8080 | 🔴 Esqueleto | Solo la estructura de carpetas vacía (`application`, `domain`, `infrastructure`); sin clases |

**Documentación detallada por subsistema:**

| Documento | Contenido |
|-----------|-----------|
| [`services/contest-platform/docs/RABBITMQ.md`](./services/contest-platform/docs/RABBITMQ.md) | Topología, contrato de mensajes con `judge-service`, garantías de entrega, DLQ y reintentos |
| [`services/contest-platform/docs/WEBSOCKET.md`](./services/contest-platform/docs/WEBSOCKET.md) | Canal `/ws/submissions`: autenticación en el handshake, contrato del mensaje, alcance por equipo y limitaciones |
| [`services/judge-service/docs/BWRAP.md`](./services/judge-service/docs/BWRAP.md) | Aislamiento de la ejecución con bubblewrap: comando, qué ve el programa, filtro seccomp y códigos de salida |
| [`services/judge-service/docs/CGROUPS.md`](./services/judge-service/docs/CGROUPS.md) | Límites y métricas por ejecución con cgroups v2: hoja por evaluación, memoria, PIDs, CPU y de dónde sale cada veredicto |
| [`services/judge-service/docs/REQUIREMENTS.md`](./services/judge-service/docs/REQUIREMENTS.md) | Lo que hay que preparar en el host: kernel, slice de cgroups y flags del contenedor |
| [`services/judge-service/docs/WORKERS.md`](./services/judge-service/docs/WORKERS.md) | Pool de workers: evaluaciones en paralelo, directorios y cgroups por worker |

---

### Módulo `auth` (`contest-platform`, puerto `8080`)
**Responsabilidad:** Gestión de identidad y autenticación. Paquete `co.uceva.contests_platform.auth`.

| Clase | Descripción |
|-------|-------------|
| `User` | Entidad JPA `users`: `id (UUID)`, `firstName`, `lastName`, `username`, `email`, `passwordHash`, `role (SUPER_ADMIN/ADMIN/ORGANIZER/STUDENT)`, `institution`, `isActive`, `createdAt` |

**Roles del sistema (enum `Role`):**

| Rol | Descripción |
|-----|-------------|
| `SUPER_ADMIN` | Dueño de la plataforma. Único que asigna y retira administradores. Se crea al arrancar `contest-platform` con `SUPER_ADMIN_EMAIL`/`SUPER_ADMIN_PASSWORD`; no se obtiene por registro ni por la API. Los demás servicios lo tratan como `ADMIN`. |
| `ADMIN` | Administrador. Gestiona estudiantes y organizadores (no a otros administradores). |
| `ORGANIZER` | Organizador. Gestiona problemas y concursos. |
| `STUDENT` | Estudiante. Solo lectura pública y sus propios envíos. |
| *`SERVICE`* | Rol técnico. **No está en el enum** `Role` del módulo `auth` porque no es un usuario humano. `judge-service` lo firma directamente en sus tokens vía `ServiceTokenProvider`. |

**Estructura:** capas planas (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `security`, `cache`, `config`), sin interfaces de casos de uso ni adaptadores. Detalle en [`services/contest-platform/docs/AUTH_API.md`](./services/contest-platform/docs/AUTH_API.md).

| Clase | Responsabilidad |
|-------|-----------------|
| `AuthService` | Registro (email en minúsculas, unicidad sin distinguir mayúsculas), login con bloqueo por intentos fallidos, logout (lista negra + borrado del refresh token) y refresh con rotación |
| `ProfileService` | Consulta de perfil, cambio de username y de contraseña (esta última cierra todas las sesiones) y disponibilidad de username |
| `AdminUserService` | Listado paginado con búsqueda, cambio de rol y de estado, con reglas de jerarquía (el rol del actor se lee de la base de datos) |
| `JwtService` | Emite y lee JWT (JJWT 0.12.5, `jwt.secret` / `jwt.issuer`). Cada token lleva `type` = `access` o `refresh` |
| `AuthCache` | Redis: `auth:failed_attempts:*`, `auth:lock:*`, `auth:refresh_token:*`, `auth:blacklist:<sha256>`. Implementa `TokenBlacklist` |

**Endpoints activos:**

| Método | Endpoint | Acceso | Request Body | Respuesta |
|--------|----------|--------|-------------|----------|
| POST | `/api/v1/auth/register` | Público | `{firstName, lastName, username, email, password, institution?}` | 201 `{message, userId}` |
| POST | `/api/v1/auth/login` | Público | `{email, password}` | 200 `{accessToken, refreshToken, expiresIn, type: "Bearer"}` |
| POST | `/api/v1/auth/refresh` | Público | `{refreshToken}` | 200 igual que login |
| POST | `/api/v1/auth/logout` | Bearer | — | 200 siempre |
| GET | `/api/v1/auth/me` | Bearer | — | 200 perfil |
| PATCH | `/api/v1/auth/me/username` | Bearer | `{username}` | 200 perfil · 409 en uso |
| PATCH | `/api/v1/auth/me/password` | Bearer | `{currentPassword, newPassword}` | 204 · 422 contraseña actual incorrecta |
| GET | `/api/v1/auth/check-username?username=` | Público | — | 200 `{available}` |
| GET | `/api/v1/admin/users?page&size&search` | ADMIN / SUPER_ADMIN | — | 200 página de usuarios |
| PATCH | `/api/v1/admin/users/{id}/role` | ADMIN / SUPER_ADMIN | `{role}` | 200 usuario · 403 jerarquía · 404 |
| PATCH | `/api/v1/admin/users/{id}/status` | ADMIN / SUPER_ADMIN | `{isActive}` | 200 usuario · 403 (incl. autodesactivación) · 404 |

**Validaciones:**
- `firstName`, `lastName`: obligatorios, 2–50 caracteres.
- `username`: 3–30 caracteres (letras, dígitos, `_`, `.`).
- `email`: obligatorio, formato válido.
- `password` / `newPassword`: mínimo 8 caracteres, al menos 1 mayúscula, 1 número y 1 carácter especial.
- `institution`: opcional.

**Comportamiento del login:**
- Login fallido: incrementa `auth:failed_attempts:<email>` (TTL de 1 hora desde el primer fallo).
- Tras **5 intentos fallidos**: bloqueo con `auth:lock:<email>` durante **15 minutos**.
- Cuenta bloqueada: el login responde `423` con los minutos restantes.

**BD:** Tabla `users` (PostgreSQL), creada por la migración Flyway `V7`.  
**Redis:** `auth:failed_attempts:<email>` (TTL 1h) · `auth:lock:<email>` (TTL 15min) · `auth:refresh_token:<userId>` (TTL 8h) · `auth:blacklist:<sha256 del token>` (TTL = vigencia restante)

---

### Módulo `problem` (`contest-platform`, puerto `8080`)
**Responsabilidad:** CRUD de problemas algorítmicos y sus casos de prueba. Paquete `co.uceva.contests_platform.problem`.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Problem` | `id`, `createdBy`, `title`, `statement`, `timeLimitMs`, `memoryLimitKb`, `difficulty (value object)`, `inputFormat`, `outputFormat` |
| `TestCase` | `id`, `problemId`, `expectedOutput`, `orderIndex`, `isSample`, `input`, `output` |

**Value objects:**

| Value Object | Validación |
|-------------|-----------|
| `Difficulty` | Entero entre 1 y 10 |
| `TimeLimit` | Entero positivo (ms) |
| `MemoryLimit` | Entero positivo (KB) |

**Casos de uso implementados:**

| Caso de uso | Descripción |
|-------------|-------------|
| `CreateProblemUseCase` | Crea un problema; `createdBy` viene del JWT |
| `UpdateProblemUseCase` | Actualiza campos de un problema existente |
| `DeleteProblemUseCase` | Elimina problema y sus test cases (CASCADE en BD) |
| `GetProblemByIdUseCase` | Busca por UUID |
| `GetAllProblemsUseCase` | Lista todos los problemas |
| `GetAllProblemsByTitleUseCase` | Búsqueda por coincidencia de título |
| `CreateTestCaseUseCase` | Crea un test case individual |
| `CreateTestCaseBatchUseCase` | Crea varios test cases en una sola transacción |
| `UpdateTestCaseUseCase` | Actualiza un test case existente |
| `DeleteTestCaseUseCase` | Elimina un test case por UUID |
| `DeleteTestCaseBatchUseCase` | Elimina una lista de test cases por sus UUIDs |
| `ReorderTestCasesUseCase` | Reordena los `orderIndex` de los test cases de un problema |
| `GetTestCaseByIdUseCase` | Busca test case por UUID (privado: requiere rol) |
| `GetAllTestCaseByProblemIdUseCase` | Lista todos los test cases de un problema (privado) |
| `GetAllSampleTestCasesByProblemIdUseCase` | Lista solo los test cases con `isSample=true` (público) |

**Endpoints activos:**

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/v1/problems` | ADMIN, ORGANIZER | Crear problema |
| GET | `/api/v1/problems/{id}` | Público | Obtener problema por ID |
| GET | `/api/v1/problems/all` | Público | Listar todos los problemas |
| GET | `/api/v1/problems/title/{title}` | Público | Buscar por título |
| PUT | `/api/v1/problems/{id}` | ADMIN, ORGANIZER | Actualizar problema |
| DELETE | `/api/v1/problems/{id}` | ADMIN, ORGANIZER | Eliminar problema |
| POST | `/api/v1/problems/test-cases/{problemId}` | ADMIN, ORGANIZER | Crear test case |
| POST | `/api/v1/problems/test-cases/{problemId}/batch` | ADMIN, ORGANIZER | Crear test cases en lote |
| POST | `/api/v1/problems/test-cases/batch-delete` | ADMIN, ORGANIZER | Eliminar test cases en lote |
| GET | `/api/v1/problems/test-cases/{problemId}/all` | ADMIN, ORGANIZER, SERVICE | Listar todos los test cases (incluye privados) |
| GET | `/api/v1/problems/test-cases/{problemId}/samples` | Público | Listar solo los test cases de ejemplo |
| GET | `/api/v1/problems/test-cases/{id}` | ADMIN, ORGANIZER, SERVICE | Obtener test case por ID |
| PUT | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN, ORGANIZER | Actualizar test case |
| PUT | `/api/v1/problems/test-cases/{problemId}/reorder` | ADMIN, ORGANIZER | Reordenar test cases |
| DELETE | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN, ORGANIZER | Eliminar test case |

> El rol `SERVICE` en los endpoints de test cases permite a `judge-service` leer los casos de prueba privados durante la evaluación.

**Seguridad:** la de `contest-platform` (ver §6): un único `JwtAuthenticationFilter` y `@EnableMethodSecurity`. Los `@PreAuthorize` aceptan `ADMIN` y `SUPER_ADMIN` juntos.

**BD:** Tablas `problems` y `test_cases`, creadas por la migración `V11__Create_problems_and_test_cases_tables.sql`.

---

### Módulo `submission` (`contest-platform`, puerto `8080`)
**Responsabilidad:** Recibir envíos de código, validar su elegibilidad, persistirlos y orquestar el ciclo de evaluación vía mensajería. Paquete `co.uceva.contests_platform.submission`.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Submission` | `id`, `teamId`, `problemId`, `language (ProgrammingLanguage)`, `sourceCode (SourceCode)`, `verdict (VerdictStatus)`, `status (SubmissionStatus)`, `executionTimeMs`, `memoryUsedKb`, `codeSizeBytes`, `submittedAt` |

**Casos de uso implementados (12 total):**
- `SubmitCodeUseCase` — Valida, detecta duplicados y persiste
- `EnqueueSubmissionUseCase` — Publica `SubmissionReceivedEvent` → RabbitMQ, publisher confirms
- `UpdateSubmissionVerdictUseCase` — Actualiza veredicto y métricas al recibir `SubmissionJudgedEvent`
- `MarkSubmissionJudgingUseCase` — Transiciona a `JUDGING` al recibir `SubmissionJudgingStartedEvent`
- `MarkSubmissionEnqueueErrorUseCase` — Transiciona a `ENQUEUE_ERROR` en DLQ
- `NotifySubmissionStatusUseCase` — Empuja cambios de estado por WebSocket
- `GetSubmissionByIdUseCase`, `GetAllSubmissionsUseCase`, `GetSubmissionsByProblemUseCase`, `GetSubmissionsByTeamUseCase`, `GetSubmissionHistoryUseCase`, `GetSubmissionMetricsUseCase`, `DeleteSubmissionUseCase`

**Puertos de salida:**

| Puerto | Implementación activa |
|--------|----------------------|
| `SubmissionEventPublisher` | `RabbitSubmissionEventPublisherAdapter` si `app.messaging.enabled=true`; `NoOpSubmissionEventPublisherAdapter` en otro caso |
| `SubmissionStatusNotifier` | `WebSocketSubmissionStatusNotifierAdapter` |
| `TeamMembershipPort` | `TeamMembershipPortImpl` — consulta `GetTeamByIdUseCase` del módulo `contest` y devuelve los integrantes reales del equipo (destinatarios del WebSocket) |
| `ContestDataPort` | `ContestServiceDataAdapter` — equipo, concurso (horario) y problemas del concurso, leídos in-process desde los casos de uso del módulo `contest` |

**Elegibilidad del envío (`SubmissionEligibilityPolicy`):** un envío solo se acepta si el equipo existe, el usuario autenticado es integrante de ese equipo, el concurso del equipo está activo (según `startTime`/`endTime` y el reloj) y el problema pertenece a ese concurso. Rige igual para todos los roles. El concurso no viaja en la petición: se deduce del equipo. Si la regla falla se lanza `SubmissionNotAllowedException` con el motivo.

**Topología completa de RabbitMQ (declarada por `contest-platform`):**

| Recurso | Nombre | Tipo | Notas |
|---------|--------|------|-------|
| Exchange principal | `submission.exchange` | Topic, durable | Exchange único para todo el pipeline |
| Exchange de DLX | `submission.dlx` | Direct, durable | Recibe mensajes rechazados de todas las colas |
| Cola de evaluación | `submission.evaluate` | Durable | Consumida por `judge-service` (prefetch=1) |
| DLQ de evaluación | `submission.evaluate.dlq` | Durable | `ExhaustedSubmissionDeadLetterListener` la consume |
| Cola de judging | `submission.judging` | Durable | Consumida por `contest-platform` (notificación JUDGING) |
| DLQ de judging | `submission.judging.dlq` | Durable | Mensajes no procesables de judging |
| Cola de juzgado | `submission.judged` | Durable | Consumida por `contest-platform` (actualiza veredicto) |
| DLQ de juzgado | `submission.judged.dlq` | Durable | Mensajes no procesables de judged |

**Routing keys:**

| Clave | Origen → Destino |
|-------|------------------|
| `submission.evaluate` | `contest-platform` → `judge-service` |
| `submission.judging` | `judge-service` → `contest-platform` |
| `submission.judged` | `judge-service` → `contest-platform` |

**Política de reintentos del consumidor (listener retry):**
- Max intentos: 3 · Intervalo inicial: 1s · Multiplicador: 2 · Máximo: 10s
- `default-requeue-rejected=false` — los mensajes agotados van a la DLQ, no se reencolan.

**Reintento de envíos PENDING (`PendingSubmissionRetryScheduler`):**
- Intervalo: cada 30 s (`app.submission.retry.interval-ms=30000`)
- Grace period: 15 s — envíos más recientes que esto no se reencolan aún
- Batch: máximo 50 envíos por ciclo (`app.submission.retry.batch-size=50`)

**Endpoints activos:**

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/v1/submissions` | STUDENT, ADMIN, ORGANIZER | Enviar solución |
| GET | `/api/v1/submissions/{id}` | STUDENT, ADMIN, ORGANIZER, SERVICE | Obtener envío por ID |
| GET | `/api/v1/submissions` | ADMIN, ORGANIZER | Listar todos los envíos |
| GET | `/api/v1/submissions/problem/{problemId}` | ADMIN, ORGANIZER, SERVICE | Envíos de un problema |
| GET | `/api/v1/submissions/team/{teamId}` | STUDENT, ADMIN, ORGANIZER | Envíos de un equipo |
| GET | `/api/v1/submissions/history` | STUDENT, ADMIN, ORGANIZER | Historial equipo+problema (`?problemId=&teamId=`) |
| GET | `/api/v1/submissions/{id}/metrics` | STUDENT, ADMIN, ORGANIZER, SERVICE | Métricas de un envío |
| DELETE | `/api/v1/submissions/{id}` | ADMIN, ORGANIZER | Eliminar envío |

**BD:** Tabla `submissions`, con tres migraciones Flyway dentro de la numeración global del servicio:  
- `V8` — Tabla base con check constraints de `language` y `verdict`  
- `V9` — Columna `status` con ciclo de vida `PENDING/QUEUED/JUDGING/JUDGED`  
- `V10` — Amplió el check constraint de `status` para incluir `ENQUEUE_ERROR`  
`JPA_DDL_AUTO` vale `update` por defecto (ver §11). Historial Flyway: `flyway_schema_history_contest`, compartido por todos los módulos.

---

### `judge-service` — Puerto `8084`
**Responsabilidad:** Motor de evaluación (Judging Engine). Consume la cola de RabbitMQ, ejecuta el código en un sandbox aislado con `bubblewrap` y emite el veredicto.

| Elemento de dominio | Descripción |
|---------------------|-------------|
| `JudgeTask`         | submissionId, language, sourceCode, testCases, timeLimit, memoryLimit. Se ensambla con `JudgeTask.create(...)` y valida sus invariantes (al menos un caso de prueba, límites en rango) |
| `JudgeResult`       | submissionId, verdict, executionTimeMs, memoryUsedKb, failedTestCase (`null` si es `ACCEPTED` o si falló antes de ejecutar, p. ej. compilación). Se crea con `JudgeResult.create(...)` |
| `TestCase`          | VO propio del juez: id, input, expectedOutput, orderIndex. No comparte modelo con el módulo `problem` |
| Límites del sandbox | VOs validados: `TimeLimit`, `MemoryLimit`, `PidsLimit`, `VolumeSizeLimit`, `OutputSizeLimit`, `ErrorSizeLimit`, `HardTimePercent`, `WatchIntervalMillis`, `AbsoluteTimeLimit` |

**Puertos de dominio y excepciones:**
- `TestCaseRepository` (`domain/repository`) — lectura de los casos de prueba de un problema; los datos pertenecen al módulo `problem` de `contest-platform`.
- `TestCasesNotFoundException` — el problema no tiene casos de prueba.
- `SandboxExecutionException` — fallo del propio sistema en el sandbox, distinto de un veredicto que culpa al estudiante.

**Caso de uso:**
- `EvaluateSubmissionUseCase` — recibe el `SubmissionReceivedEvent`, obtiene casos de prueba y límites, ensambla el `JudgeTask`, lo ejecuta en el sandbox y publica el `JudgeResult`. Los fallos del sistema se propagan sin publicar, para que la mensajería reintente y, agotados los reintentos, lo cierre la DLQ.

**Puertos de salida (`application/port/out`):** `ProblemLimitsPort` (límites de tiempo y memoria del problema), `SandboxExecutor` (ejecuta la tarea y devuelve el `JudgeResult`) y `JudgeResultPublisher` (publica el resultado hacia `submission-service`).

> La compilación (`Compiler`) está pendiente y se abordará en otra historia de usuario.

**Infraestructura (`infrastructure/`):**

| Pieza | Rol |
|-------|-----|
| `messaging/SubmissionEvaluationListener` | Consume `submission.evaluate`. Un fallo transitorio se propaga para el reintento (3 intentos, luego DLQ); uno permanente (`TestCasesNotFoundException`, `IllegalArgumentException`) se rechaza sin reintentar |
| `messaging/RabbitJudgeResultPublisherAdapter` | Publica `SubmissionJudgedEvent` en `submission.exchange` con routing key `submission.judged`, persistente y esperando la confirmación del broker |
| `security/ServiceTokenProvider` | Emite el JWT `role=SERVICE` del juez (HS256, secreto y emisor compartidos, `sub` = `app.security.jwt.service-id`, vigencia 5 min) y lo reutiliza hasta 30 s antes de expirar. Exige `JWT_SECRET` (mín. 32 bytes, sin valor por defecto) |
| `client/ProblemServiceTestCaseRepositoryAdapter` | `TestCaseRepository` sobre `GET /api/v1/problems/test-cases/{id}/all` (JWT `SERVICE` en cada petición); un `404` o lista vacía es `TestCasesNotFoundException` |
| `client/ProblemServiceLimitsAdapter` | `ProblemLimitsPort` sobre `GET /api/v1/problems/{id}` |
| `sandbox/RunnerSandboxExecutor` | `SandboxExecutor` sobre `Runner`: escribe el fuente a un temporal, ejecuta y traduce el mapa de resultados a `JudgeResult` |
| `web/MonitorLimitsController` | Consulta y modificación de los límites de los monitores del sandbox (solo `ADMIN`) |
| `security/JwtAuthenticationFilter` + `config/SecurityConfig` | Autentica el HTTP con el JWT y el mismo `JwtTokenValidator` que el resto; el rol pasa a `ROLE_<rol>` para `@PreAuthorize` |
| `persistence/InMemoryMonitorLimitsRepository` | Guarda los límites vigentes en memoria (el juez no tiene BD) |
| `config/` | Convertidor JSON de RabbitMQ, bean del `Runner` y `RestClient` hacia `contest-platform` (`app.problem-service.base-url`, variable `PROBLEM_SERVICE_URL`) |

**Componentes del sandbox:**
- `BwrapCommandFactory` — construye el comando `bwrap` con namespaces, bind mounts y límites de cgroups
- `Runner` — lanza el proceso en el sandbox, captura stdout/stderr con monitores
- `Compiler` — compila el código antes de ejecutar (pendiente para C/C++/Java)
- `RunnerSandboxExecutor` — adaptador que orquesta `Compiler` + `Runner` por cada test case
- `TimeWatchdog` — termina el proceso si supera `timeLimitMs`
- `OutputHandle` / `ErrorsHandle` — capturan y limitan stdout/stderr
- `CgroupKiller` — mata el proceso y sus hijos vía cgroups v2
- `SandboxWorkspace` — crea/limpia el directorio temporal por envío

**MonitorLimits (ajustables en caliente, en memoria — no persisten):**

| Límite | Propiedad | Default |
|--------|-----------|--------|
| `outputSizeBytes` | `app.sandbox.monitor.output-size-bytes` | 10 MB |
| `errorSizeBytes` | `app.sandbox.monitor.error-size-bytes` | 10 MB |
| `hardTimePercent` | `app.sandbox.monitor.hard-time-percent` | 0.3 (30 %) |
| `watchIntervalMs` | `app.sandbox.monitor.watch-interval-ms` | 500 ms |
| `absoluteTimeMs` | `app.sandbox.monitor.absolute-time-ms` | 10 000 ms |

**Seguridad:** `JwtAuthenticationFilter` activo. `judge-service` tiene su propia `SecurityConfig` que exige autenticación en todas las rutas excepto `/actuator/health`.

**Autenticación S2S:** `ServiceTokenProvider` auto-genera tokens `role=SERVICE` firmados con la misma clave compartida. El token se renueva automáticamente 30 s antes de expirar. `TTL` configurable con `app.security.jwt.service-token-ttl-seconds` (default 300 s).

**Sin BD:** Excluye `DataSourceAutoConfiguration` y `HibernateJpaAutoConfiguration`. Los límites del monitor se guardan en `InMemoryMonitorLimitsRepository` y se pierden al reiniciar.

---

### Módulo `feedback` (`contest-platform`, puerto `8080`)
**Responsabilidad:** Generar retroalimentación educativa inteligente usando un LLM externo (OpenAI / Gemini) tras un veredicto de evaluación.

> 🔴 **Estado actual:** Paquete `co.uceva.contests_platform.feedback` con las carpetas de capas vacías (`.gitkeep`). Sin dominio, aplicación ni infraestructura implementados.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Feedback` | `id`, `submissionId`, `aiSuggestion`, `generatedAt` |

**Casos de uso planeados:**
- `GenerateFeedbackUseCase` — Construye prompt con (código + veredicto + métricas) y llama a la API LLM
- `GetFeedbackUseCase` — Recupera retroalimentación ya generada para un envío

**Restricciones de negocio:**
- El LLM **no debe revelar** la solución del problema, solo orientar al estudiante.
- Solo se genera feedback cuando el veredicto **no** es `ACCEPTED`.
- El módulo consumirá `SubmissionJudgedEvent` desde RabbitMQ (mismo exchange `submission.exchange`).
- Depende de un EIF externo (API LLM) — su disponibilidad está sujeta al proveedor.

**Tablas BD planeadas:** `feedback`

---

### Módulo `contest` (`contest-platform`, puerto `8080`)
**Responsabilidad:** Maratones de programación, equipos, invitaciones, aclaraciones y scoreboard. Paquete `co.uceva.contests_platform.contest`.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Contest`          | `id`, `createdBy`, `password`, `title`, `startTime`, `endTime`, `status`, `description` |
| `Team`             | `id`, `contestId`, `captainId`, `score`, `penaltyTime`, `nameTeam`, integrantes (`memberUserIds`) |
| `Member`           | `teamId`, `userId`, `registeredAt` |
| `Invitation`       | Invitación de un capitán a un usuario para unirse al equipo, con `InvitationStatus` |
| `ContestProblem`   | `contestId`, `problemId`, `points`, `penalty`, `orderIndex`, `alias`, `balloonColor` |
| `Clarification`    | `id`, `problemId`, `teamId`, `question`, `answer`, `status`, `createdAt`, `answeredAt` |

**Endpoints activos** (prefijo `/api/v1`; `*` = `ADMIN`, `SUPER_ADMIN` u `ORGANIZER` salvo que se indique otro acceso):

| Método | Endpoint | Acceso |
|--------|----------|--------|
| POST / PUT / DELETE | `/contests`, `/contests/{id}` | ADMIN, ORGANIZER |
| GET | `/contests`, `/contests/{id}`, `/contests/title/{title}`, `/contests/creator/{createdBy}` | Público |
| POST / PUT / DELETE | `/contests/{contestId}/problems`, `/contest-problems/{id}` | ADMIN, ORGANIZER |
| GET | `/contests/{contestId}/problems` | Público |
| POST | `/contests/{contestId}/teams` | Autenticado |
| GET | `/contests/{contestId}/teams`, `/contests/{contestId}/scoreboard`, `/teams`, `/teams/{id}`, `/teams/user/{userId}`, `/teams/{id}/members` | Público |
| PUT | `/teams/{id}` | Autenticado |
| DELETE | `/teams/{id}` | ADMIN, ORGANIZER |
| POST | `/teams/{id}/members` | ADMIN, ORGANIZER |
| DELETE | `/teams/{id}/members/{userId}` | Autenticado |
| POST | `/teams/{teamId}/invitations` | Autenticado |
| GET | `/invitations/me` | Autenticado |
| POST | `/invitations/{id}/accept`, `/invitations/{id}/reject` | Autenticado |
| GET / POST | `/invitations/{id}/respond` | Público (enlace del correo; responde HTML) |
| POST | `/contest-problems/{problemId}/clarifications` | Autenticado |
| GET | `/contests/{contestId}/clarifications` | Público |
| POST | `/clarifications/{id}/answer` | ADMIN, ORGANIZER |

**Casos de uso** (`contest/application/usecase`): Create/Update/Delete/Get de concursos, de equipos y de problemas de concurso; `InviteMember`, `AcceptInvitation`, `RejectInvitation`, `GetInvitationsByUser`; `AddMember`, `RemoveMember`; `AskClarification`, `AnswerClarification`, `GetClarificationsByContest`; `GetScoreboard`.

**Infraestructura propia:** `SmtpEmailSender` y `InvitationLinks` (correo de invitación con enlace a `/invitations/{id}/respond`, usa `MAIL_*` y `APP_PUBLIC_URL`) y `ProblemServiceCatalogAdapter` (comprueba que un problema exista al asignarlo a un maratón, llamando al caso de uso del módulo `problem` in-process).

**Integración con `submission`:** el módulo `submission` lee equipos, horarios del concurso y problemas del concurso llamando directamente a `GetTeamByIdUseCase`, `GetContestByIdUseCase` y `GetProblemsByContestUseCase`.

**Tablas BD:** `contests`, `teams`, `members`, `invitations`, `contest_problems`, `clarifications` (Flyway `V1`–`V6`).

---

## 6. Seguridad y autorización

### 6.1 Modelo de roles

| Rol | Tipo de actor | Permisos principales |
|-----|---------------|----------------------|
| `ADMIN` | Usuario humano | Acceso total. |
| `ORGANIZER` | Usuario humano | Gestión de problemas y concursos. |
| `STUDENT` | Usuario humano | Lectura pública, envío de soluciones, consulta de sus propios envíos. |
| `SERVICE` | Microservicio | Rol técnico S2S. `judge-service` lo usa para leer test cases privados del módulo `problem`. No existe en el enum `Role` de `auth-service`. |

### 6.2 Patrón de seguridad implementado (`contest-platform` y `judge-service`)

Ambos servicios implementan el mismo patrón:

1. **`JwtTokenValidator`** (bean): `contest-platform` lo declara en `auth/config/SecurityConfig`; `judge-service` en su propia configuración. Lee `app.security.jwt.secret` y `app.security.jwt.issuer`. La clave **no tiene valor por defecto** (`JWT_SECRET`): un secreto heredado en producción aceptaría tokens falsificados. Si falta la variable de entorno, el servicio no arranca.

2. **`JwtAuthenticationFilter`** (extiende `OncePerRequestFilter`): Extrae el header `Authorization: Bearer <token>`, llama a `JwtTokenValidator.validate()`, convierte el claim `role` en la authority `ROLE_<rol>` y lo publica en `SecurityContextHolder`. Si el token es inválido o está ausente, la petición **continúa como anónima** — el filtro no la rechaza.

3. **`SecurityConfig`** (`@Configuration @EnableWebSecurity @EnableMethodSecurity`): Registra el filtro antes de `UsernamePasswordAuthenticationFilter`. Configura la cadena como stateless y sin CSRF. En `contest-platform` añade CORS (`CORS_ALLOWED_ORIGINS`). Establece `anyRequest().permitAll()` — la autorización real la imponen los `@PreAuthorize` en cada método del controlador.

**Ejemplo de uso de `@PreAuthorize`:**
```java
// Solo ADMIN, SUPER_ADMIN u ORGANIZER pueden ver todos los envios
@GetMapping
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ORGANIZER')")
public ResponseEntity<List<SubmissionResponseDTO>> getAll() { ... }

// El propio motor de juicio (SERVICE) puede leer test cases privados
@GetMapping("/{problemId}/all")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ORGANIZER','SERVICE')")
public ResponseEntity<List<TestCaseResponseDTO>> getAll(...) { ... }
```

> `JwtTokenValidator` vive en `common-infrastructure` sin anotaciones Spring. Cada servicio lo instancia como `@Bean`.

### 6.3 Estado de seguridad del módulo `auth`

El módulo `auth` emite los tokens, así que `/api/v1/auth/login`, `/register`, `/refresh` y `/check-username` son públicos. Los endpoints de perfil (`/me/**`) validan el access token que reciben (firma, emisor, tipo `access`, lista negra y cuenta activa). Los refresh tokens llevan `type=refresh` y `JwtTokenValidator` los rechaza como credencial de acceso. Como `auth` ya comparte proceso con el resto de módulos, la validación de los demás endpoints es la del filtro único de §6.2.

### 6.4 Autenticación servicio a servicio (S2S)

`judge-service` usa `ServiceTokenProvider` para auto-emitir tokens `role=SERVICE`. Este componente:
- Firma tokens JJWT con la misma clave compartida y el mismo emisor (`JWT_ISSUER`).
- Renueva el token automáticamente 30 segundos antes de que expire (`REFRESH_MARGIN`).
- El identificador del servicio viene de `app.security.jwt.service-id` (UUID configurable).
- TTL por defecto: 5 minutos (`app.security.jwt.service-token-ttl-seconds=300`).

---

## 7. Canal WebSocket (notificaciones en tiempo real)

`contest-platform` (módulo `submission`) expone un WebSocket en `/ws/submissions` para empujar cambios de estado de los envíos.

| Aspecto | Decisión |
|---------|----------|
| Sentido | **Unidireccional.** El servidor empuja; lo que el cliente envíe se descarta. |
| Autenticación | JWT validado en el handshake por `JwtHandshakeInterceptor`. Token rechazado → handshake devuelve `401 Unauthorized`. |
| Alcance | La conexión se asocia al `sub` del token. Solo se entregan cambios del mismo usuario. |
| Subprotocolo confirmado | `goslint-judge` (`app.websocket.submission.sub-protocol`) |
| Destinatarios | Resueltos vía `TeamMembershipPort` (`TeamMembershipPortImpl`): los integrantes reales del equipo dueño del envío. |

### 7.1 Extracción del token en el handshake

Los navegadores no permiten enviar headers HTTP personalizados al abrir un WebSocket. Por eso `JwtHandshakeInterceptor` soporta **tres vías**, en orden de prioridad:

1. **Subprotocolo `bearer.<token>`** — El cliente anuncia `["goslint-judge", "bearer.<token>"]` en `Sec-WebSocket-Protocol`. El servidor confirma solo `goslint-judge` y extrae el token del segundo subprotocolo. Esta es la vía recomendada desde el navegador.
2. **Header `Authorization: Bearer <token>`** — Válida en entornos no-browser (clientes Java, scripts, tests).
3. **Query param `?token=<token>`** — Último recurso. No recomendado en producción porque el token queda en los logs del servidor.

```javascript
// Ejemplo desde el frontend (vía subprotocolo)
const ws = new WebSocket(
  'ws://localhost:8080/ws/submissions',
  ['goslint-judge', `bearer.${accessToken}`]
);
```

### 7.2 Registro de sesiones

`WebSocketSessionRegistry` mantiene un `Map<UUID, WebSocketSession>` en memoria, donde la clave es el `userId` del token. Una vez establecida la sesión, `WebSocketSubmissionStatusNotifierAdapter` la busca para empujar mensajes.

> ⚠️ El registro es **local a la instancia**. Con más de una réplica del servicio, un usuario conectado a la instancia A no recibirá los mensajes generados en la instancia B. Para escalar horizontalmente se requiere un registro compartido en Redis.

### 7.3 Mensaje emitido (`SubmissionStatusEventDTO`)

```json
{
  "type": "SUBMISSION_STATUS_UPDATED",
  "submissionId": "3fa85f64-...",
  "problemId": "3fa85f64-...",
  "teamId": "3fa85f64-...",
  "status": "JUDGED",
  "verdict": "ACCEPTED",
  "executionTimeMs": 120,
  "memoryUsedKb": 2048,
  "occurredAt": "2026-08-25T18:30:00Z"
}

---

## 8. Flujo de datos principal (envío de código)

```
[Cliente/Estudiante]
    │  POST /api/v1/submissions  (HTTP, JWT requerido)
    ▼
[contest-platform :8080]
    │  1. JwtAuthenticationFilter valida el JWT
    │  2. @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER')")
    │  3. SubmitCodeUseCase: valida la elegibilidad (integrante del equipo, concurso activo, problema del concurso), detecta duplicados, persiste (status: PENDING, verdict: PENDING)
    │  4. EnqueueSubmissionUseCase: publica SubmissionReceivedEvent → RabbitMQ
    │     (exchange: submission.exchange, routing-key: submission.evaluate)
    │  5. Con publisher confirm → marca Submission como QUEUED
    │     (sin confirm → queda PENDING, PendingSubmissionRetryScheduler lo reintenta)
    │
    ▼
[judge-service :8084]  ← Consumidor de "submission.evaluate"
    │  6. SubmissionEvaluationListener recibe el evento
    │  7. Publica SubmissionJudgingStartedEvent → contest-platform (routing: submission.judging)
    │  8. ServiceTokenProvider provee token role=SERVICE
    │  9. Recupera test cases desde contest-platform, módulo problem (autenticado como SERVICE)
    │  10. RunnerSandboxExecutor: ejecuta código en bwrap sandbox
    │  11. Compila (C, C++, Java) y ejecuta; determina el veredicto
    │  12. Publica SubmissionJudgedEvent → RabbitMQ (routing: submission.judged)
    │
    ├─▶ [contest-platform] ← Consumidor de "submission.judged"
    │     13. UpdateSubmissionVerdictUseCase: actualiza veredicto + métricas (status: JUDGED)
    │     14. NotifySubmissionStatusUseCase: empuja estado por WebSocket al usuario
    │
    └─▶ [contest-platform, módulo feedback] ← PENDIENTE DE IMPLEMENTAR
           15. Llamada API LLM (si veredicto ≠ ACCEPTED)
           16. Persistir Feedback
```

**Mecanismos de resiliencia:**
- **DLQ:** Mensajes rechazados en `submission.evaluate.dlq` → `ExhaustedSubmissionDeadLetterListener` cierra el envío con `ENQUEUE_ERROR` y notifica por WebSocket.
- **Reintento:** `PendingSubmissionRetryScheduler` barre filas en `PENDING` periódicamente y las reencola.

---

## 9. Convenciones de paquetes Java

El prefijo base es `co.uceva.contests_platform.<módulo>` en `contest-platform` (`auth`, `problem`, `contest`, `submission`) y `co.uceva.<servicio>` en el resto.

> **El módulo `auth`** no sigue la estructura de abajo: usa capas planas (`controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `security/`, `cache/`, `config/`). El esquema de puertos y adaptadores descrito a continuación aplica a los demás servicios que lo usan.

```
co.uceva.<servicio>/   (ejemplo genérico de la estructura por capas)
├── domain/
│   ├── model/          ← Clases de entidad (User.java, AuthToken.java)
│   ├── exception/      ← Excepciones de dominio (UserAlreadyExistsException.java)
│   └── repository/     ← Puertos de salida de repositorio (UserRepository.java)
│   └── service/        ← Puertos de servicios técnicos (TokenProvider.java, PasswordEncoder.java)
├── application/
│   └── usecase/        ← Interfaces (LoginUserUseCase.java)
│   └── usecase/impl/   ← Implementaciones (LoginUserUseCaseImpl.java)
└── infrastructure/
    ├── web/
    │   ├── controller/ ← @RestController (AuthController.java)
    │   └── dto/        ← Request/Response DTOs
    ├── persistence/
    │   ├── adapter/    ← Adaptadores (UserRepositoryAdapter.java)
    │   ├── entity/     ← @Entity JPA (UserJpaEntity.java)
    │   └── repository/ ← Spring Data Interfaces (SpringDataUserRepository.java)
    ├── cache/          ← Adaptadores Redis (RedisCacheAdapter.java)
    ├── security/       ← Adaptadores de seguridad (JwtTokenProviderAdapter.java)
    └── config/         ← @Configuration (SecurityConfig.java)
```

### Reglas de nomenclatura

| Tipo de clase | Sufijo recomendado | Ejemplo |
|--------------|-------------------|---------|
| Caso de uso (interfaz) | `UseCase` | `LoginUserUseCase` |
| Implementación de caso de uso | `UseCaseImpl` | `LoginUserUseCaseImpl` |
| Puerto de salida (repositorio) | `Repository` | `UserRepository` |
| Puerto de salida (servicio técnico) | `Port` | `TeamMembershipPort` |
| Controlador REST | `Controller` | `AuthController` |
| JPA Entity | `JpaEntity` | `UserJpaEntity` |
| Adaptador de repositorio | `RepositoryAdapter` / `RepositoryImpl` | `UserRepositoryAdapter` |
| Adaptador de caché | `CacheAdapter` | `RedisCacheAdapter` |
| DTO de request | `Request` / `RequestDTO` | `LoginRequest`, `SubmitCodeRequestDTO` |
| DTO de response | `Response` / `ResponseDTO` | `LoginResponse`, `SubmissionResponseDTO` |
| Mapper web | `WebMapper` | `SubmissionWebMapper` |
| Mapper de entidad | `EntityMapper` | `SubmissionEntityMapper` |

---

## 10. Configuración de Gradle

### `build.gradle` raíz
Gestiona el BOM de Spring Boot 3.2.4 y configuración común a todos los sub-proyectos:
- `io.freefair.lombok:8.6` — Lombok aplicado a todos
- `jacoco:0.8.12` — Cobertura de código
- `spring-boot-starter-test` + `spring-security-test` — Base de tests
- Lee el archivo `.env` del proyecto raíz para inyectarlo como variables de entorno al ejecutar `bootRun`

### `settings.gradle`
Declara los 2 módulos shared y 2 servicios:
```gradle
include 'shared:common-domain'
include 'shared:common-infrastructure'
include 'services:judge-service'
include 'services:contest-platform'
```

### `build.gradle` por servicio

| Servicio | Dependencias específicas |
|---------|--------------------------|
| `contest-platform` | `common-domain`, `common-infrastructure`, `spring-boot-starter-data-jpa`, `-security`, `-amqp`, `-websocket`, `-mail`, `flyway-core`, `postgresql`, H2 (tests), `spring-rabbit-test`, Lombok explícito |
| `judge-service` | `common-domain`, `common-infrastructure`, `spring-boot-starter-amqp`, `spring-boot-starter-actuator`, `spring-rabbit-test` |

> `common-infrastructure/build.gradle` usa `api` (no `implementation`) para exponer Spring Security, Redis, JJWT y Spring Web a todos los servicios que lo dependan.

### Gradle Wrapper
Ya está en el repositorio (`gradlew`, `gradle/wrapper/`). Se usa `./gradlew` desde `backend/`; no hace falta Gradle instalado globalmente.

---

## 11. Problemas conocidos y deuda técnica

| # | Problema | Criticidad | Acción requerida |
|---|----------|-----------|------------------|
| 1 | ~~`auth-service` sin `/logout`~~ | ✅ Resuelto | `POST /logout` revoca el access token (lista negra en Redis) y borra el refresh token |
| 2 | ~~`auth-service` sin Flyway~~ | ✅ Resuelto | La tabla `users` se crea con `V7`. Pendiente: `JPA_DDL_AUTO` sigue en `update` por defecto en `application.properties`; pasar a `validate` |
| 3 | **Propiedades JWT duplicadas** | 🟡 Parcial | `contest-platform` define `jwt.secret`/`jwt.issuer` (emisión) y `app.security.jwt.*` (validación) a la vez. Unificar en `app.security.jwt.*` |
| 4 | ~~El sandbox de `judge-service` no soporta C, C++ y Java~~ | ✅ Resuelto | HU-34: el juez evalúa Python, C, C++ y Java |
| 5 | ~~`JwtTokenValidator` sin lista negra~~ | ✅ Resuelto | Constructor opcional con `TokenBlacklist`; el módulo `auth` la implementa con `AuthCache` |
| 6 | **El módulo `feedback` es un esqueleto** — Toda su lógica está pendiente | 🔴 Alta | Implementar según roadmap del proyecto |
| 7 | ~~Residuos de la migración al monolito~~ | ✅ Resuelto | `submission-service/` y `users-service/` eliminados |
| 8 | **Registro de sesiones WebSocket es local a la instancia** — No escala horizontalmente | 🟡 Media | Compartir el registro vía Redis cuando se requiera más de una réplica |
| 9 | **`traefik.yml` está vacío** — Sin API Gateway cada servicio se expone por su puerto | 🟡 Media | Configurar Traefik con reenvío de `Upgrade`/`Sec-WebSocket-Protocol` |
| 10 | **`app.websocket.allowed-origins=*`** en `contest-platform` | 🟡 Media | Restringir a los orígenes del frontend antes de exponer |
| 11 | **Clases de arranque de prueba con nombre del servicio viejo** (`ProblemServiceApplicationTests`, `SubmissionServiceApplicationTests` en `contest-platform`) | 🟢 Baja | Unificarlas en un único `ContestsPlatformApplicationTests` |
| 12 | **`MonitorLimits` de `judge-service` no persisten** — Los límites del sandbox se pierden en cada reinicio | 🟢 Baja | Añadir persistencia (Redis o BD) si se quieren límites configurables persistentes |
| 13 | **`judge-service` llama por HTTP al módulo `problem`** y `PROBLEM_SERVICE_URL` tiene por defecto `localhost:8082`, un puerto que ya no existe. Además `ProblemServiceClientConfig` (módulo `contest`) es un bean sin uso que aún exige `app.problem-service.base-url`, y `APP_PUBLIC_URL` apunta por defecto a `:8086` | 🔴 Alta | Apuntar `PROBLEM_SERVICE_URL` y `APP_PUBLIC_URL` a `:8080`; borrar `ProblemServiceClientConfig` y su propiedad |
| 14 | **`docker-compose.yml`, `start-services.sh`, `stop-services.sh` y los `rewrites` de `student-app`** siguen describiendo los antiguos auth/problem/submission/contest-service (puertos 8081–8086) | 🔴 Alta | Reemplazar esos servicios por `contest-platform` (8080) |

---

## 12. Cómo agregar un nuevo microservicio

> Si el sistema necesita escalar con un nuevo servicio (ej. `notification-service`):

1. **Crear la carpeta** en `services/`:
   ```
   services/notification-service/
   └── src/main/java/co/uceva/notification/
       ├── domain/
       ├── application/
       └── infrastructure/
   ```

2. **Crear `build.gradle`**:
   ```gradle
   plugins {
       id 'org.springframework.boot'
   }
   dependencies {
       implementation project(':shared:common-infrastructure')
       // + dependencias específicas
   }
   ```

3. **Registrar en `settings.gradle`**:
   ```gradle
   include 'services:notification-service'
   ```

4. **Crear clase principal** `NotificationServiceApplication.java`:
   ```java
   package co.uceva.notification;
   @SpringBootApplication
   public class NotificationServiceApplication {
       public static void main(String[] args) {
           SpringApplication.run(NotificationServiceApplication.class, args);
       }
   }
   ```

5. **Crear `application.properties`** con al menos:
   ```properties
   spring.application.name=notification-service
   server.port=8087
   app.security.jwt.secret=${JWT_SECRET}
   app.security.jwt.issuer=${JWT_ISSUER:goslint-judge}
   ```

6. **Añadir `JwtConfig` + `JwtAuthenticationFilter` + `SecurityConfig`** siguiendo el patrón de `judge-service` o de `contest-platform`.

---

## 13. Checklist antes de hacer un PR

Antes de enviar cambios en cualquier microservicio, verificar:

- [ ] Las entidades en `domain/` **no importan** ninguna clase de Spring Boot, JPA o cualquier framework.
- [ ] Los casos de uso en `application/` **no importan** clases de `infrastructure/`.
- [ ] Los controladores REST están en `infrastructure/web/` y usan DTOs, **no** entidades de dominio directamente.
- [ ] Las implementaciones de repositorios JPA están en `infrastructure/persistence/`.
- [ ] Se creó una migración de Flyway en `src/main/resources/db/migration/` para cualquier cambio de esquema (si el servicio tiene BD; en `contest-platform` la numeración `V<n>` es única y global para todos los módulos).
- [ ] El nombre del paquete de test coincide con el paquete del código fuente (`co.uceva.contests_platform.<módulo>` o `co.uceva.<servicio>`).
- [ ] El `build.gradle` del servicio tiene el plugin `org.springframework.boot` aplicado.
- [ ] Se añadió la dependencia correcta en `settings.gradle` si se creó un nuevo módulo.
- [ ] Las propiedades de JWT siguen la convención: `app.security.jwt.secret` y `app.security.jwt.issuer` sin valores por defecto.
- [ ] Los tests pasan con `./gradlew :services:<servicio>:test` desde la raíz del backend.

---

*Documento actualizado el 2026-11-07, tras la migración a monolito modular (`contest-platform`). Actualizar cuando cambien decisiones arquitectónicas.*
