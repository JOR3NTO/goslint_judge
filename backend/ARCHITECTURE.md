# Goslint Judge — Guía de Arquitectura del Backend

> **Trabajo de Grado** | Sistema de Evaluación Automática de Código Fuente para Maratones de Programación – UCEVA  
> **Stack**: Java 17 · Spring Boot 3.2.4 · Gradle (Multi-project) · PostgreSQL · RabbitMQ · Redis · Docker  
> **Patrón**: Arquitectura Limpia (Clean Architecture) + Microservicios REST

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
| Spring Boot    | Framework de cada microservicio                                  |
| PostgreSQL     | Persistencia relacional (BD compartida con schemas por servicio) |
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
├── settings.gradle               ← Declara los 8 sub-proyectos Gradle
├── gradle.properties             ← Variables globales
├── .env                          ← Variables de entorno locales (no commitear)
│
├── shared/                       ← Librerías internas reutilizables
│   ├── common-domain/            ← Enums y eventos que cruzan más de un servicio
│   │   └── build.gradle          ← Sin dependencias externas; solo Lombok
│   └── common-infrastructure/    ← Seguridad base (JWT) reutilizable por todos los servicios
│       └── build.gradle          ← api: spring-web, spring-security, spring-data-redis, jjwt
│
└── services/                     ← Cada directorio = un microservicio desplegable
    ├── auth-service/             ← Puerto 8081
    ├── problem-service/          ← Puerto 8082
    ├── submission-service/       ← Puerto 8083
    ├── judge-service/            ← Puerto 8084
    ├── feedback-service/         ← Puerto 8085 (esqueleto)
    ├── contest-service/          ← Puerto 8086 (esqueleto)
    └── users-service/            ← OBSOLETO — duplicado de auth-service (pendiente eliminar)
```

Cada microservicio activo tiene la siguiente forma interna:

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
- Ejemplo: `Submission.java`, `User.java`, `Problem.java`, `JudgeTask.java`.

#### `application/` — Casos de uso
- Interfaces (puertos): `SubmissionRepository`, `JudgeResultPublisher`, `AuthCacheRepository`.
- Implementaciones de los casos de uso: `SubmitCodeUseCaseImpl`, `LoginUserUseCaseImpl`.
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
| `SubmissionReceivedEvent` | Record | Publicado por `submission-service` al encolar un envío. Contiene: `submissionId`, `teamId`, `problemId`, `language`, `sourceCode`, `submittedAt` |
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

| Servicio | Puerto | Estado | Descripción real del estado actual |
|----------|--------|--------|------------------------------------|
| `auth-service` | 8081 | 🟡 Parcial | `POST /register` y `POST /login` funcionales. Emite JWT (Access 15 min, Refresh 8h). Bloqueo de cuenta por 5 intentos fallidos (Redis, 15 min). `ddl-auto=update` + Flyway desactivado. **Falta:** endpoint `/logout` con invalidación de token, migrar a Flyway + `validate`, añadir filtro JWT para proteger rutas futuras. |
| `problem-service` | 8082 | 🟢 Funcional | CRUD completo de problemas y test cases con `@PreAuthorize` por roles. Filtro JWT activo (`JwtAuthenticationFilter`). Flyway con `V1__Create_problems_test_cases.sql`. `ddl-auto=validate`. |
| `submission-service` | 8083 | 🟢 Funcional | Ciclo completo: recepción, encolamiento RabbitMQ con publisher confirms, consumo del veredicto, DLQ con cierre `SYSTEM_ERROR/ENQUEUE_ERROR`, reintento de pendientes (`PendingSubmissionRetryScheduler`), notificación WebSocket. Filtro JWT activo en HTTP y en handshake WS. Flyway `V1`, `V2`, `V3`. `ddl-auto=validate`. |
| `judge-service` | 8084 | 🟡 Parcial | Motor de evaluación operativo. Sandbox con `bubblewrap`. Soporta: Python completo. C/C++/Java: **compilación pendiente** (actualmente devuelven `JUDGE_ERROR` sin culpar al estudiante). Consume `submission.evaluate`, publica `submission.judging` y `submission.judged`. Filtro JWT activo (`@PreAuthorize` con rol `ADMIN`). `ServiceTokenProvider`: autoservicio de tokens `role=SERVICE` para leer test cases de `problem-service`. Sin BD ni Flyway (no persiste nada). |
| `feedback-service` | 8085 | 🔴 Esqueleto | Solo la clase de arranque `FeedbackServiceApplication`. Sin dominio, aplicación ni infraestructura. |
| `contest-service` | 8086 | 🔴 Esqueleto | Solo la clase de arranque `ContestServiceApplication`. Sin dominio, aplicación ni infraestructura. |

---

### `auth-service` — Puerto `8081`
**Responsabilidad:** Gestión de identidad y autenticación.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `User` | `id (UUID)`, `firstName`, `lastName`, `username`, `email`, `passwordHash`, `role (ADMIN/STUDENT/ORGANIZER)`, `institution`, `isActive`, `createdAt` |
| `AuthToken` | `accessToken`, `refreshToken`, `expiresIn` |

**Roles del sistema (enum `Role`):**

| Rol | Descripción |
|-----|-------------|
| `ADMIN` | Administrador. Acceso total. |
| `ORGANIZER` | Organizador. Gestiona problemas y concursos. |
| `STUDENT` | Estudiante. Solo lectura pública y sus propios envíos. |
| *`SERVICE`* | Rol técnico. **No está en el enum** `Role` de `auth-service` porque no es un usuario humano. `judge-service` lo firma directamente en sus tokens vía `ServiceTokenProvider`. |

**Casos de uso implementados:**

| Caso de uso | Implementación | Descripción |
|-------------|----------------|-------------|
| `RegisterUserUseCase` | `RegisterUserUseCaseImpl` | Valida unicidad de email y username, hashea contraseña (BCrypt), persiste con rol `STUDENT` |
| `LoginUserUseCase` | `LoginUserUseCaseImpl` | Verifica intentos fallidos (Redis), valida credenciales, genera Access + Refresh Token, guarda refresh en Redis |

**Puertos de dominio:**

| Puerto | Implementación |
|--------|---------------|
| `UserRepository` | `UserRepositoryAdapter` → `SpringDataUserRepository` (JPA) |
| `AuthCacheRepository` | `RedisCacheAdapter` — gestiona: `auth:failed_attempts:*`, `auth:lock:*`, `auth:refresh_token:*` |
| `TokenProvider` | `JwtTokenProviderAdapter` — usa JJWT 0.12.5, configura con `jwt.secret` y `jwt.issuer` |
| `PasswordEncoder` | Clase anónima en `SecurityConfig` → BCryptPasswordEncoder |

**Endpoints activos:**

| Método | Endpoint | Acceso | Request Body | Respuesta |
|--------|----------|--------|-------------|----------|
| POST | `/api/v1/auth/register` | Público | `{firstName, lastName, username, email, password, institution?}` | 201 `{message, userId}` |
| POST | `/api/v1/auth/login` | Público | `{email, password}` | 200 `{accessToken, refreshToken, expiresIn, type: "Bearer"}` |

**Validaciones del `RegisterRequest`:**
- `firstName`, `lastName`: obligatorios, 2–50 caracteres.
- `username`: obligatorio, 3–50 caracteres.
- `email`: obligatorio, formato válido.
- `password`: obligatorio, mínimo 8 caracteres, al menos 1 mayúscula, 1 número y 1 carácter especial.
- `institution`: opcional.

**Errores conocidos (comportamiento actual):**
- Login fallido (credenciales incorrectas): incrementa `auth:failed_attempts:<email>` con TTL de 1 hora.
- Tras **5 intentos fallidos**: bloqueo de cuenta con `auth:lock:<email>` durante **15 minutos**.
- Cuenta bloqueada: el login devuelve mensaje de bloqueo con el tiempo restante en segundos.

**BD:** Tabla `users` (PostgreSQL, `ddl-auto=update` — Flyway desactivado).  
**Redis:** `auth:failed_attempts:<email>` (TTL 1h) · `auth:lock:<email>` (TTL 15min) · `auth:refresh_token:<userId>` (TTL 8h)

> ⚠️ **Deuda técnica:** `auth-service` no usa Flyway ni `ddl-auto=validate`. El esquema `users` depende de que Hibernate lo cree o migre automáticamente.

---

### `problem-service` — Puerto `8082`
**Responsabilidad:** CRUD de problemas algorítmicos y sus casos de prueba.

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
| POST | `/api/v1/test-cases/{problemId}` | ADMIN, ORGANIZER | Crear test case |
| POST | `/api/v1/test-cases/{problemId}/batch` | ADMIN, ORGANIZER | Crear test cases en lote |
| POST | `/api/v1/test-cases/batch-delete` | ADMIN, ORGANIZER | Eliminar test cases en lote |
| GET | `/api/v1/test-cases/{problemId}/all` | ADMIN, ORGANIZER, SERVICE | Listar todos los test cases (incluye privados) |
| GET | `/api/v1/test-cases/{problemId}/samples` | Público | Listar solo los test cases de ejemplo |
| GET | `/api/v1/test-cases/{id}` | ADMIN, ORGANIZER, SERVICE | Obtener test case por ID |
| PUT | `/api/v1/test-cases/{problemId}/{testCaseId}` | ADMIN, ORGANIZER | Actualizar test case |
| PUT | `/api/v1/test-cases/{problemId}/reorder` | ADMIN, ORGANIZER | Reordenar test cases |
| DELETE | `/api/v1/test-cases/{problemId}/{testCaseId}` | ADMIN, ORGANIZER | Eliminar test case |

> El rol `SERVICE` en los endpoints de test cases permite a `judge-service` leer los casos de prueba privados durante la evaluación.

**Seguridad:** `JwtAuthenticationFilter` activo + `@EnableMethodSecurity`. `JwtConfig` declara `JwtTokenValidator` con `app.security.jwt.secret` y `app.security.jwt.issuer` (sin default — la ausencia de la variable de entorno aborta el arranque).

**BD:** Tablas `problems` y `test_cases`. Flyway `V1__Create_problems_test_cases.sql`. `ddl-auto=validate`. Historial en `flyway_schema_history_problem`.

---

### `submission-service` — Puerto `8083`
**Responsabilidad:** Recibir envíos de código, persistirlos y orquestar el ciclo de evaluación vía mensajería.

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
| `TeamMembershipPort` | `NoOpTeamMembershipAdapter` — resuelve cada envío como individual. Se reemplazará por un adaptador HTTP a `contest-service` cuando exista |

**Topología completa de RabbitMQ (declarada por `submission-service`):**

| Recurso | Nombre | Tipo | Notas |
|---------|--------|------|-------|
| Exchange principal | `submission.exchange` | Topic, durable | Exchange único para todo el pipeline |
| Exchange de DLX | `submission.dlx` | Direct, durable | Recibe mensajes rechazados de todas las colas |
| Cola de evaluación | `submission.evaluate` | Durable | Consumida por `judge-service` (prefetch=1) |
| DLQ de evaluación | `submission.evaluate.dlq` | Durable | `ExhaustedSubmissionDeadLetterListener` la consume |
| Cola de judging | `submission.judging` | Durable | Consumida por `submission-service` (notificación JUDGING) |
| DLQ de judging | `submission.judging.dlq` | Durable | Mensajes no procesables de judging |
| Cola de juzgado | `submission.judged` | Durable | Consumida por `submission-service` (actualiza veredicto) |
| DLQ de juzgado | `submission.judged.dlq` | Durable | Mensajes no procesables de judged |

**Routing keys:**

| Clave | Origen → Destino |
|-------|------------------|
| `submission.evaluate` | `submission-service` → `judge-service` |
| `submission.judging` | `judge-service` → `submission-service` |
| `submission.judged` | `judge-service` → `submission-service` |

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

**BD:** Tabla `submissions`. Flyway con 3 migraciones:  
- `V1` — Tabla base con check constraints de `language` y `verdict`  
- `V2` — Columna `status` con ciclo de vida `PENDING/QUEUED/JUDGING/JUDGED/ENQUEUE_ERROR`  
- `V3` — Amplió el check constraint de `status` para incluir `ENQUEUE_ERROR`  
`ddl-auto=validate`. Historial en `flyway_schema_history_submission`.

---

### `judge-service` — Puerto `8084`
**Responsabilidad:** Motor de evaluación (Judging Engine). Consume la cola de RabbitMQ, ejecuta el código en un sandbox aislado con `bubblewrap` y emite el veredicto.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `JudgeTask` | `submissionId`, `language`, `sourceCode`, `testCases`, `timeLimitMs`, `memoryLimitKb` |
| `JudgeResult` | `submissionId`, `verdict (VerdictStatus)`, `executionTimeMs`, `memoryUsedKb` |
| `TestCase` | `id`, `input`, `expectedOutput`, `timeLimit`, `memoryLimit` |
| `MonitorLimits` | Límites ajustables en caliente para los monitores del sandbox |

**Value Objects:**

| Value Object | Descripción |
|-------------|-------------|
| `AbsoluteTimeLimit` | Tiempo máximo absoluto de ejecución (ms) |
| `HardTimePercent` | Porcentaje del time limit a partir del cual se activa el vigilante |
| `MemoryLimit` | Límite de memoria (MB) |
| `OutputSizeLimit` | Tamaño máximo del stdout (bytes) |
| `ErrorSizeLimit` | Tamaño máximo del stderr (bytes) |
| `PidsLimit` | Máximo de procesos hijo (anti fork-bomb) |
| `WatchIntervalMillis` | Intervalo de muestreo del vigilante (ms) |
| `VolumeSizeLimit` | Tamaño máximo del volumen temporal del sandbox |

**Casos de uso implementados:**

| Caso de uso | Descripción |
|-------------|-------------|
| `EvaluateSubmissionUseCase` | Orquesta el pipeline: recupera test cases de `problem-service`, ejecuta en sandbox, determina veredicto |
| `GetMonitorLimitsUseCase` | Devuelve los límites actuales del sandbox (solo ADMIN) |
| `UpdateMonitorLimitsUseCase` | Actualiza los límites en caliente (solo ADMIN) |

**Puertos de salida:**

| Puerto | Implementación |
|--------|---------------|
| `SandboxExecutor` | `RunnerSandboxExecutor` → `Runner` + `Compiler` + `BwrapCommandFactory` |
| `TestCaseRepository` | `ProblemServiceTestCaseRepositoryAdapter` — REST a `problem-service` |
| `ProblemLimitsPort` | `ProblemServiceLimitsAdapter` — REST a `problem-service` |
| `JudgeResultPublisher` | `RabbitJudgeResultPublisherAdapter` — publica `submission.judged` |
| `SubmissionJudgingNotifier` | `RabbitSubmissionJudgingNotifierAdapter` — publica `submission.judging` |

**Endpoints activos:**

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/v1/judge/monitor-limits` | ADMIN | Consultar límites del sandbox en caliente |
| PUT | `/api/v1/judge/monitor-limits` | ADMIN | Actualizar límites del sandbox en caliente |
| GET | `/actuator/health` | Público | Health check |

**Sandbox:**

| Lenguaje | Estado | Notas |
|----------|--------|-------|
| Python | ✅ Completo | Ejecución directa con `python3` en bwrap |
| C | 🟡 Pendiente compilación | `Compiler.java` no implementado; devuelve `JUDGE_ERROR` |
| C++ | 🟡 Pendiente compilación | Ídem |
| Java | 🟡 Pendiente compilación | Ídem |

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

### `feedback-service` — Puerto `8085`
**Responsabilidad:** Generar retroalimentación educativa inteligente usando un LLM externo (OpenAI / Gemini) tras un veredicto de evaluación.

> 🔴 **Estado actual:** Solo clase de arranque `FeedbackServiceApplication`. Sin dominio, aplicación ni infraestructura implementados.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Feedback` | `id`, `submissionId`, `aiSuggestion`, `generatedAt` |

**Casos de uso planeados:**
- `GenerateFeedbackUseCase` — Construye prompt con (código + veredicto + métricas) y llama a la API LLM
- `GetFeedbackUseCase` — Recupera retroalimentación ya generada para un envío

**Restricciones de negocio:**
- El LLM **no debe revelar** la solución del problema, solo orientar al estudiante.
- Solo se genera feedback cuando el veredicto **no** es `ACCEPTED`.
- El servicio consumirá `SubmissionJudgedEvent` desde RabbitMQ (mismo exchange `submission.exchange`).
- Depende de un EIF externo (API LLM) — su disponibilidad está sujeta al proveedor.

**Tablas BD planeadas:** `feedback`

---

### `contest-service` — Puerto `8086`
**Responsabilidad:** Creación y gestión de maratones de programación (concursos), equipos y scoreboard.

> 🔴 **Estado actual:** Solo clase de arranque `ContestServiceApplication`. Sin dominio, aplicación ni infraestructura implementados. Mientras no exista, `submission-service` resuelve cada equipo como individual mediante `NoOpTeamMembershipAdapter`.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Contest` | `id`, `createdBy`, `password`, `title`, `startTime`, `endTime`, `status`, `description` |
| `Team` | `id`, `contestId`, `score`, `penaltyTime`, `nameTeam` |
| `Member` | `teamId`, `userId`, `registeredAt` |
| `ContestProblem` | `contestId`, `problemId`, `points`, `orderIndex`, `alias` |

**Casos de uso planeados:**
- `CreateContestUseCase` — Solo ORGANIZER/ADMIN
- `RegisterTeamUseCase` — Inscripción de equipos con contraseña
- `GetScoreboardUseCase` — Ranking en tiempo real (via WebSocket/SSE)
- `AddProblemToContestUseCase` — Asignación de problemas al concurso

**Integración con otros servicios:**
- Al implementarse, `submission-service` debe cambiar `app.team-membership.provider` y añadir un adaptador HTTP que consulte a `contest-service` para resolver los integrantes de un equipo (destinatarios de las notificaciones WebSocket).

**Tablas BD planeadas:** `contests`, `teams`, `members`, `contest_problems`

---

## 6. Seguridad y autorización

### 6.1 Modelo de roles

| Rol | Tipo de actor | Permisos principales |
|-----|---------------|----------------------|
| `ADMIN` | Usuario humano | Acceso total. |
| `ORGANIZER` | Usuario humano | Gestión de problemas y concursos. |
| `STUDENT` | Usuario humano | Lectura pública, envío de soluciones, consulta de sus propios envíos. |
| `SERVICE` | Microservicio | Rol técnico S2S. `judge-service` lo usa para leer test cases privados de `problem-service`. No existe en el enum `Role` de `auth-service`. |

### 6.2 Patrón de seguridad implementado (`problem-service`, `submission-service`, `judge-service`)

Cada uno de estos servicios implementa exactamente el mismo patrón en tres clases:

1. **`JwtConfig`** (`@Configuration`): Declara el bean `JwtTokenValidator(secret, issuer)` leyendo `app.security.jwt.secret` y `app.security.jwt.issuer`. La clave **no tiene valor por defecto**: un secreto heredado en producción aceptaría tokens falsificados. Si falta la variable de entorno, el servicio no arranca.

2. **`JwtAuthenticationFilter`** (extiende `OncePerRequestFilter`): Extrae el header `Authorization: Bearer <token>`, llama a `JwtTokenValidator.validate()`, convierte el claim `role` en la authority `ROLE_<rol>` y lo publica en `SecurityContextHolder`. Si el token es inválido o está ausente, la petición **continúa como anónima** — el filtro no la rechaza.

3. **`SecurityConfig`** (`@Configuration @EnableWebSecurity @EnableMethodSecurity`): Registra el filtro antes de `UsernamePasswordAuthenticationFilter`. Configura la cadena como stateless (`SessionCreationPolicy.STATELESS`) y sin CSRF. Establece `anyRequest().permitAll()` — la autorización real la imponen los `@PreAuthorize` en cada método del controlador.

**Ejemplo de uso de `@PreAuthorize`:**
```java
// Solo ADMIN puede ver todos los envios
@GetMapping
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
public ResponseEntity<List<SubmissionResponseDTO>> getAll() { ... }

// ADMIN, ORGANIZER y el propio motor de juicio pueden leer test cases
@GetMapping("/{problemId}/all")
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")
public ResponseEntity<List<TestCaseResponseDTO>> getAll(...) { ... }
```

> `JwtTokenValidator` vive en `common-infrastructure` sin anotaciones Spring. Cada servicio lo instancia como `@Bean` en su propia configuración.

### 6.3 Estado de seguridad de `auth-service`

`auth-service` **no** implementa el filtro JWT en sus endpoints. Sus rutas `/api/v1/auth/**` son públicas intencionalmente, porque es el emisor de tokens, no el validador. La configuración actual en `SecurityConfig` usa `anyRequest().permitAll()` globalmente.

### 6.4 Autenticación servicio a servicio (S2S)

`judge-service` usa `ServiceTokenProvider` para auto-emitir tokens `role=SERVICE`. Este componente:
- Firma tokens JJWT con la misma clave compartida y el mismo emisor (`JWT_ISSUER`).
- Renueva el token automáticamente 30 segundos antes de que expire (`REFRESH_MARGIN`).
- El identificador del servicio viene de `app.security.jwt.service-id` (UUID configurable).
- TTL por defecto: 5 minutos (`app.security.jwt.service-token-ttl-seconds=300`).

---

## 7. Canal WebSocket (notificaciones en tiempo real)

`submission-service` expone un WebSocket en `/ws/submissions` para empujar cambios de estado de los envíos.

| Aspecto | Decisión |
|---------|----------|
| Sentido | **Unidireccional.** El servidor empuja; lo que el cliente envíe se descarta. |
| Autenticación | JWT validado en el handshake por `JwtHandshakeInterceptor`. Token rechazado → handshake devuelve `401 Unauthorized`. |
| Alcance | La conexión se asocia al `sub` del token. Solo se entregan cambios del mismo usuario. |
| Subprotocolo confirmado | `goslint-judge` (`app.websocket.submission.sub-protocol`) |
| Destinatarios | Resueltos vía `TeamMembershipPort`. `NoOpTeamMembershipAdapter` activo mientras no exista `contest-service`. |

### 7.1 Extracción del token en el handshake

Los navegadores no permiten enviar headers HTTP personalizados al abrir un WebSocket. Por eso `JwtHandshakeInterceptor` soporta **tres vías**, en orden de prioridad:

1. **Subprotocolo `bearer.<token>`** — El cliente anuncia `["goslint-judge", "bearer.<token>"]` en `Sec-WebSocket-Protocol`. El servidor confirma solo `goslint-judge` y extrae el token del segundo subprotocolo. Esta es la vía recomendada desde el navegador.
2. **Header `Authorization: Bearer <token>`** — Válida en entornos no-browser (clientes Java, scripts, tests).
3. **Query param `?token=<token>`** — Último recurso. No recomendado en producción porque el token queda en los logs del servidor.

```javascript
// Ejemplo desde el frontend (vía subprotocolo)
const ws = new WebSocket(
  'ws://localhost:8083/ws/submissions',
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
[submission-service :8083]
    │  1. JwtAuthenticationFilter valida el JWT
    │  2. @PreAuthorize("hasAnyRole('STUDENT','ADMIN','ORGANIZER')")
    │  3. SubmitCodeUseCase: valida, detecta duplicados, persiste (status: PENDING, verdict: PENDING)
    │  4. EnqueueSubmissionUseCase: publica SubmissionReceivedEvent → RabbitMQ
    │     (exchange: submission.exchange, routing-key: submission.evaluate)
    │  5. Con publisher confirm → marca Submission como QUEUED
    │     (sin confirm → queda PENDING, PendingSubmissionRetryScheduler lo reintenta)
    │
    ▼
[judge-service :8084]  ← Consumidor de "submission.evaluate"
    │  6. SubmissionEvaluationListener recibe el evento
    │  7. Publica SubmissionJudgingStartedEvent → submission-service (routing: submission.judging)
    │  8. ServiceTokenProvider provee token role=SERVICE
    │  9. Recupera test cases desde problem-service (autenticado como SERVICE)
    │  10. RunnerSandboxExecutor: ejecuta código en bwrap sandbox
    │  11. Determina veredicto (Python: completo; C/C++/Java: compilación pendiente)
    │  12. Publica SubmissionJudgedEvent → RabbitMQ (routing: submission.judged)
    │
    ├─▶ [submission-service] ← Consumidor de "submission.judged"
    │     13. UpdateSubmissionVerdictUseCase: actualiza veredicto + métricas (status: JUDGED)
    │     14. NotifySubmissionStatusUseCase: empuja estado por WebSocket al usuario
    │
    └─▶ [feedback-service] ← PENDIENTE DE IMPLEMENTAR
           15. Llamada API LLM (si veredicto ≠ ACCEPTED)
           16. Persistir Feedback
```

**Mecanismos de resiliencia:**
- **DLQ:** Mensajes rechazados en `submission.evaluate.dlq` → `ExhaustedSubmissionDeadLetterListener` cierra el envío con `ENQUEUE_ERROR` y notifica por WebSocket.
- **Reintento:** `PendingSubmissionRetryScheduler` barre filas en `PENDING` periódicamente y las reencola.

---

## 9. Convenciones de paquetes Java

El prefijo base de todos los paquetes es `co.uceva.<servicio>`.

```
co.uceva.auth/
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
Declara los 8 sub-proyectos (nota: `users-service` está registrado pero es obsoleto):
```gradle
include 'shared:common-domain'
include 'shared:common-infrastructure'
include 'services:auth-service'
include 'services:problem-service'
include 'services:submission-service'
include 'services:judge-service'
include 'services:feedback-service'
include 'services:contest-service'
```

### `build.gradle` por servicio

| Servicio | Dependencias específicas |
|---------|--------------------------|
| `auth-service` | `common-infrastructure`, `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, Lombok explícito |
| `problem-service` | `common-infrastructure`, `flyway-core`, `postgresql`, H2 (tests) |
| `submission-service` | `common-domain`, `common-infrastructure`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-amqp`, `spring-boot-starter-websocket`, `flyway-core`, `postgresql`, H2 (tests), `spring-rabbit-test` |
| `judge-service` | `common-domain`, `common-infrastructure`, `spring-boot-starter-amqp`, `spring-boot-starter-actuator`, `spring-rabbit-test` |
| `feedback-service` | Solo `spring-boot-starter` base |
| `contest-service` | Solo `spring-boot-starter` base |

> `common-infrastructure/build.gradle` usa `api` (no `implementation`) para exponer Spring Security, Redis, JJWT y Spring Web a todos los servicios que lo dependan.

### Gradle Wrapper
Ya está en el repositorio (`gradlew`, `gradle/wrapper/`). Se usa `./gradlew` desde `backend/`; no hace falta Gradle instalado globalmente.

---

## 11. Problemas conocidos y deuda técnica

| # | Problema | Criticidad | Acción requerida |
|---|----------|-----------|------------------|
| 1 | **`auth-service` no tiene endpoint `/logout`** — El token emitido no puede ser invalidado antes de su expiración | 🔴 Alta (HU-03) | Implementar `POST /api/v1/auth/logout`: blacklist del token en Redis. Ver plan detallado en `plan_hu03_logout.md` |
| 2 | **`auth-service` usa `ddl-auto=update` sin Flyway** — El esquema de `users` no está versionado | 🟡 Media | Añadir `V1__Create_users_table.sql` a Flyway (ya existe en `db/migration/` pero Flyway está desactivado), cambiar a `ddl-auto=validate` y activar Flyway |
| 3 | **`jwt.issuer` en `auth-service` usa propiedad `jwt.issuer` mientras los otros servicios usan `app.security.jwt.issuer`** — Inconsistencia en los nombres de propiedades | 🟡 Media | Unificar la propiedad a `app.security.jwt.issuer` en `auth-service` también |
| 4 | **El sandbox de `judge-service` no soporta compilación de C, C++ y Java** — Python es el único lenguaje evaluado correctamente | 🔴 Alta | Implementar `Compiler.java` para los tres lenguajes compilados. Conectar a `RunnerSandboxExecutor`. |
| 5 | **`JwtTokenValidator` en `common-infrastructure` no soporta lista negra** — Aunque Redis está disponible como dependencia, no hay integración | 🟡 Media | Añadir constructor opcional con `TokenBlacklist`. Necesario para HU-03 |
| 6 | **`contest-service` y `feedback-service` son esqueletos** — Toda su lógica está pendiente | 🔴 Alta | Implementar según roadmap del proyecto |
| 7 | **`users-service/` convive con `auth-service`** — Módulo obsoleto | 🟢 Baja | Decidir cuál queda y retirar el otro de `settings.gradle` |
| 8 | **Registro de sesiones WebSocket es local a la instancia** — No escala horizontalmente | 🟡 Media | Compartir el registro vía Redis cuando se requiera más de una réplica |
| 9 | **`traefik.yml` está vacío** — Sin API Gateway cada servicio se expone por su puerto | 🟡 Media | Configurar Traefik con reenvío de `Upgrade`/`Sec-WebSocket-Protocol` |
| 10 | **`app.websocket.allowed-origins=*`** en `submission-service` | 🟡 Media | Restringir a los orígenes del frontend antes de exponer |
| 11 | **Test package incorrecto** en `auth-service` | 🟢 Baja | La clase de test usa `co.uceva.judge.auth_service` en lugar de `co.uceva.auth` |
| 12 | **`MonitorLimits` de `judge-service` no persisten** — Los límites del sandbox se pierden en cada reinicio | 🟢 Baja | Añadir persistencia (Redis o BD) si se quieren límites configurables persistentes |

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

6. **Añadir `JwtConfig` + `JwtAuthenticationFilter` + `SecurityConfig`** siguiendo el patrón de `problem-service`.

---

## 13. Checklist antes de hacer un PR

Antes de enviar cambios en cualquier microservicio, verificar:

- [ ] Las entidades en `domain/` **no importan** ninguna clase de Spring Boot, JPA o cualquier framework.
- [ ] Los casos de uso en `application/` **no importan** clases de `infrastructure/`.
- [ ] Los controladores REST están en `infrastructure/web/` y usan DTOs, **no** entidades de dominio directamente.
- [ ] Las implementaciones de repositorios JPA están en `infrastructure/persistence/`.
- [ ] Se creó una migración de Flyway en `src/main/resources/db/migration/` para cualquier cambio de esquema (si el servicio tiene BD).
- [ ] El nombre del paquete de test coincide con el paquete del código fuente (`co.uceva.<servicio>`).
- [ ] El `build.gradle` del servicio tiene el plugin `org.springframework.boot` aplicado.
- [ ] Se añadió la dependencia correcta en `settings.gradle` si se creó un nuevo módulo.
- [ ] Las propiedades de JWT siguen la convención: `app.security.jwt.secret` y `app.security.jwt.issuer` sin valores por defecto.
- [ ] Los tests pasan con `./gradlew :<servicio>:test` desde la raíz del backend.

---

*Documento actualizado el 2026-09-30. Última revisión completa del código fuente a la fecha. Actualizar cuando cambien decisiones arquitectónicas.*
