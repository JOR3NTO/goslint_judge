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
6. [Flujo de datos principal (envío de código)](#6-flujo-de-datos-principal-envío-de-código)
7. [Convenciones de paquetes Java](#7-convenciones-de-paquetes-java)
8. [Configuración de Gradle](#8-configuración-de-gradle)
9. [Problemas conocidos y deuda técnica](#9-problemas-conocidos-y-deuda-técnica)
10. [Cómo agregar un nuevo microservicio](#10-cómo-agregar-un-nuevo-microservicio)
11. [Checklist antes de hacer un PR](#11-checklist-antes-de-hacer-un-pr)

---

## 1. Visión general del sistema

El **Goslint Judge** es un juez en línea (Online Judge System) institucional diseñado para que la UCEVA gestione de forma autónoma su repositorio de problemas algorítmicos y sus maratones de programación. Los estudiantes envían soluciones en C, C++, Java o Python; el sistema las evalúa en contenedores Docker aislados (sandbox) y emite veredictos oficiales (`Accepted`, `Wrong Answer`, `Time Limit Exceeded`, etc.), además de una retroalimentación descriptiva generada por un LLM externo.

### Componentes tecnológicos principales

| Tecnología     | Rol                                                             |
|----------------|-----------------------------------------------------------------|
| Spring Boot    | Framework de cada microservicio                                  |
| PostgreSQL     | Persistencia relacional (una BD por servicio)                   |
| RabbitMQ       | Cola de mensajes asíncrona para el pipeline de evaluación       |
| Redis          | Caché (scoreboard en tiempo real, rate-limiting)                |
| Docker SDK     | Creación y destrucción de contenedores para el sandboxing       |
| API LLM        | Retroalimentación inteligente (OpenAI / Gemini)                 |
| Traefik        | API Gateway / reverse proxy / balanceo de carga                 |
| JWT            | Autenticación stateless entre clientes y servicios              |
| Flyway         | Migraciones de esquema de base de datos                         |

---

## 2. Mapa del monorepo

```
backend/
├── build.gradle                  ← Configuración raíz (Spring Boot BOM, Lombok, tests)
├── settings.gradle               ← Declara todos los sub-proyectos Gradle
├── gradle.properties             ← Variables globales (versiones, flags)
│
├── shared/                       ← Librerías internas reutilizables
│   ├── common-domain/            ← Entidades/eventos que cruzan más de un servicio
│   │   └── build.gradle          ← Sin dependencias de Spring (dominio puro)
│   └── common-infrastructure/    ← Config transversal (excepciones HTTP, seguridad base)
│       └── build.gradle          ← Depende de common-domain + Spring Web/Validation
│
└── services/                     ← Cada directorio = un microservicio desplegable
    ├── auth-service/
    ├── problem-service/
    ├── submission-service/
    ├── judge-service/
    ├── feedback-service/
    └── contest-service/
```

Cada microservicio tiene la siguiente forma interna:

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
    │       └── application.properties
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
- Ejemplo: `Submission.java`, `Verdict.java`, `Problem.java`, `User.java`.

#### `application/` — Casos de uso
- Interfaces (puertos): `SubmissionRepository`, `JudgePort`, `FeedbackPort`.
- Servicios de aplicación (implementan los casos de uso): `SubmitCodeUseCase`, `EvaluateSubmissionUseCase`.
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
Contiene elementos del dominio que son transversales a más de un microservicio (eventos de dominio, tipos de datos compartidos).

**Restricción:** No puede tener dependencias de Spring Boot ni de ningún framework externo. Solo Lombok.

Candidatos a residir aquí:
- Eventos de dominio compartidos (ej. `SubmissionEvaluatedEvent`)
- Tipos base genéricos (ej. `AggregateRoot<ID>`, `DomainEvent`)
- Enums comunes (ej. `ProgrammingLanguage`, `VerdictStatus`)

### `shared/common-infrastructure`
Contiene configuración transversal de Spring que todos los servicios reutilizan.

**Depende de:** `common-domain` + `spring-boot-starter-web` + `spring-boot-starter-validation`

Candidatos a residir aquí:
- `GlobalExceptionHandler` (`@ControllerAdvice`)
- DTOs de respuesta estandarizados (`ApiResponse<T>`, `ErrorResponse`)
- Configuración base de seguridad JWT (filtros comunes)
- Utilidades de paginación y serialización

---

## 5. Microservicios — Responsabilidades y dominio

### Estado de implementación

| Servicio | Puerto | Estado | Qué hay hoy |
|----------|--------|--------|-------------|
| `auth-service` | 8081 | 🟡 Parcial | Registro de usuarios (`POST /api/v1/auth/register`). **La emisión de JWT sigue pendiente**: no hay login, y sin él ningún otro servicio puede autenticar de verdad una petición HTTP. Esquema aún con `ddl-auto=update`, sin Flyway |
| `problem-service` | 8082 | 🟢 Funcional | CRUD completo de problemas y casos de prueba, restricciones por rol con `@PreAuthorize`, endpoint público de *samples*. Falta el filtro JWT y su migración base de Flyway |
| `submission-service` | 8083 | 🟢 Funcional | Ciclo completo: recepción del envío, encolamiento en RabbitMQ con confirmación del broker, consumo del veredicto, cierre por error del sistema desde las DLQ, reintento de pendientes y notificación en tiempo real por WebSocket. Esquema versionado con Flyway (`V1`–`V3`). Es el único servicio que **autentica de verdad**, y solo en el handshake del WebSocket |
| `judge-service` | 8084 | 🟡 Parcial | Dominio, aplicación e infraestructura completos: consume `submission.evaluate`, obtiene casos de prueba y límites de `problem-service` por HTTP, ejecuta en el sandbox (`bwrap`/cgroups) y publica el veredicto en `submission.judged`. **Solo evalúa Python**: el `Compiler` (C, C++, Java) va en otra HU, y esos lenguajes terminan como `SYSTEM_ERROR`. El JWT `SERVICE` hacia `problem-service` lo firma el propio juez con el secreto compartido. Se despliega en su propia imagen con `bubblewrap` y seccomp (perfil `sandbox` del compose) |
| `feedback-service` | 8085 | 🔴 Esqueleto | Solo la clase de arranque |
| `contest-service` | 8086 | 🔴 Esqueleto | Solo la clase de arranque. Mientras no exista, `submission-service` resuelve cada equipo como individual mediante `NoOpTeamMembershipAdapter` |

**Documentación detallada por subsistema:**

| Documento | Contenido |
|-----------|-----------|
| [`services/submission-service/docs/RABBITMQ.md`](./services/submission-service/docs/RABBITMQ.md) | Topología, contrato de mensajes con `judge-service`, garantías de entrega, DLQ y reintentos |
| [`services/submission-service/docs/WEBSOCKET.md`](./services/submission-service/docs/WEBSOCKET.md) | Canal `/ws/submissions`: autenticación en el handshake, contrato del mensaje, alcance por equipo y limitaciones |
| [`services/judge-service/docs/BWRAP.md`](./services/judge-service/docs/BWRAP.md) | Aislamiento de la ejecución con bubblewrap: comando, qué ve el programa, filtro seccomp y códigos de salida |
| [`services/judge-service/docs/CGROUPS.md`](./services/judge-service/docs/CGROUPS.md) | Límites y métricas por ejecución con cgroups v2: hoja por evaluación, memoria, PIDs, CPU y de dónde sale cada veredicto |
| [`services/judge-service/docs/REQUIREMENTS.md`](./services/judge-service/docs/REQUIREMENTS.md) | Lo que hay que preparar en el host: kernel, slice de cgroups y flags del contenedor |

---

### `auth-service` — Puerto `8081`
**Responsabilidad:** Gestión de identidad y autenticación.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `User`             | id, username, email, passwordHash, role (ADMIN/STUDENT/ORGANIZER), institution, isActive |
| `Token`            | JWT emitido al autenticarse |

**Roles del sistema:**

| Rol         | Descripción                                                                 |
|-------------|------------------------------------------------------------------------------|
| `ADMIN`     | Administrador del sistema. Acceso total a problemas, concursos y usuarios.  |
| `ORGANIZER` | Organizador de maratones. Puede gestionar problemas y concursos.            |
| `STUDENT`   | Estudiante o competidor. Solo lectura pública de problemas y envíos propios.|
| `SERVICE`   | Rol técnico para comunicación **microservicio-a-microservicio**. No es un usuario humano; se utiliza en JWTs emitidos por otros servicios (por ejemplo, `judge-service`) para consumir endpoints internos. |

**Casos de uso principales:**
- `RegisterUserUseCase` — Registro de nuevos usuarios
- `AuthenticateUserUseCase` — Login → devuelve JWT
- `ValidateTokenUseCase` — Validación de token (consumida por otros servicios vía HTTP interno)

**Tablas BD:** `users`

---

### `problem-service` — Puerto `8082`
**Responsabilidad:** CRUD de problemas algorítmicos y sus casos de prueba.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Problem`          | id, createdBy, title, statement, timeLimitMs, memoryLimitKb, difficulty, inputFormat, outputFormat |
| `TestCase`         | id, problemId, expectedOutput, input, output, orderIndex, isSample |

**Casos de uso principales:**
- `CreateProblemUseCase` / `UpdateProblemUseCase` / `DeleteProblemUseCase` — Solo ADMIN/ORGANIZER
- `GetProblemByIdUseCase` / `GetAllProblemsUseCase` / `GetAllProblemsByTitleUseCase` — Lectura pública
- `CreateTestCaseUseCase` / `UpdateTestCaseUseCase` / `DeleteTestCaseUseCase` / `ReorderTestCasesUseCase` — Solo ADMIN/ORGANIZER
- `GetAllTestCaseByProblemIdUseCase` — ADMIN/ORGANIZER/SERVICE (incluye casos privados; necesario para `judge-service`)
- `GetTestCaseByIdUseCase` — ADMIN/ORGANIZER/SERVICE (necesario para `judge-service`)
- `GetAllSampleTestCasesByProblemIdUseCase` — Público; retorna únicamente los casos de prueba donde `isSample = true`

**Endpoints destacados:**

| Método | Endpoint | Acceso | Notas |
|--------|----------|--------|-------|
| GET    | `/api/v1/problems/{id}` | Público | Lectura del enunciado |
| GET    | `/api/v1/problems/all` | Público | Lista de problemas |
| GET    | `/api/v1/problems/title/{title}` | Público | Búsqueda por título |
| POST   | `/api/v1/problems` | ADMIN/ORGANIZER | Crear problema |
| PUT    | `/api/v1/problems/{id}` | ADMIN/ORGANIZER | Actualizar problema |
| DELETE | `/api/v1/problems/{id}` | ADMIN/ORGANIZER | Eliminar problema |
| GET    | `/api/v1/problems/test-cases/{problemId}/all` | ADMIN/ORGANIZER/SERVICE | Lista **todos** los test cases (privados y ejemplos) |
| GET    | `/api/v1/problems/test-cases/{id}` | ADMIN/ORGANIZER/SERVICE | Obtener un test case por ID |
| GET    | `/api/v1/problems/test-cases/{problemId}/samples` | Público | Lista solo los test cases donde `isSample = true` |
| POST   | `/api/v1/problems/test-cases/{problemId}` | ADMIN/ORGANIZER | Crear test case |
| POST   | `/api/v1/problems/test-cases/{problemId}/batch` | ADMIN/ORGANIZER | Crear test cases en lote |
| PUT    | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN/ORGANIZER | Actualizar test case |
| PUT    | `/api/v1/problems/test-cases/{problemId}/reorder` | ADMIN/ORGANIZER | Reordenar test cases |
| DELETE | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN/ORGANIZER | Eliminar test case |
| POST   | `/api/v1/problems/test-cases/batch-delete` | ADMIN/ORGANIZER | Eliminar test cases en lote |

**Tablas BD:** `problems`, `test_cases`

---

### `submission-service` — Puerto `8083`
**Responsabilidad:** Recibir envíos de código de los estudiantes, persistirlos y entregarlos al motor de evaluación a través de RabbitMQ.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Submission`       | id, teamId, problemId, language, sourceCode, verdict, **status**, executionTimeMs, memoryUsedKb, codeSizeBytes, submittedAt |

**Casos de uso principales:**
- `SubmitCodeUseCase` — Valida, detecta duplicados y persiste el envío
- `EnqueueSubmissionUseCase` — Publica `SubmissionReceivedEvent` en RabbitMQ y marca el envío como encolado
- `GetSubmissionHistoryUseCase` — Historial por equipo/problema
- `GetSubmissionMetricsUseCase` — Tiempo y memoria de ejecución

#### Estado del envío (`SubmissionStatus`)

`status` es **ortogonal** a `verdict`: el veredicto dice *cómo resultó evaluado* el envío, el estado dice *dónde está* dentro del flujo. Mientras el juez trabaja, lo normal es `status = QUEUED` con `verdict = PENDING`.

| Estado    | Significado                                                          |
|-----------|----------------------------------------------------------------------|
| `PENDING` | Persistido, pero aún no entregado al motor de evaluación             |
| `QUEUED`  | Entrega confirmada por el broker (publisher confirms)                |
| `JUDGING` | El motor de evaluación tomó el envío y lo está procesando            |
| `JUDGED`  | El motor finalizó y emitió un veredicto                              |

El estado vive en la propia tabla, no en memoria: por eso una fila en `PENDING` representa trabajo de entrega sin completar y `PendingSubmissionRetryScheduler` puede recogerla y reintentar el encolamiento, incluso después de un reinicio del servicio o una caída del broker.

#### Topología de mensajería

Los nombres se declaran una sola vez en `application.properties` (`app.messaging.submission.*`); `RabbitConfig` declara la topología a partir de ellas y el publicador envía a esos mismos valores. Son también el **contrato con `judge-service`**.

| Propiedad | Valor por defecto | Rol |
|-----------|-------------------|-----|
| `app.messaging.submission.exchange` | `submission.exchange` | Topic exchange duradero |
| `app.messaging.submission.routing-key` | `submission.evaluate` | Routing key de los envíos por evaluar |
| `app.messaging.submission.queue` | `submission.evaluate` | Cola que consume `judge-service` |
| `app.messaging.submission.dead-letter-exchange` | `submission.dlx` | Destino de los mensajes rechazados |
| `app.messaging.submission.dead-letter-queue` | `submission.evaluate.dlq` | Retención de los envíos que no se pudieron procesar |

`app.messaging.enabled=false` permite levantar el servicio sin broker (pruebas, desarrollo local): se usa un publicador *no-op* y no se declara la topología.

**Tablas BD:** `submissions`

#### Esquema y migraciones

El servicio arranca con `spring.jpa.hibernate.ddl-auto=validate`: **Hibernate no crea ni modifica nada**, solo comprueba que la tabla coincide con `SubmissionEntity` y aborta el arranque si no es así. El esquema lo define Flyway en `src/main/resources/db/migration/`:

| Migración | Qué hace |
|-----------|----------|
| `V1__crear_tabla_submissions.sql` | Línea base de la tabla `submissions` (tal y como existía antes del encolamiento) más los índices de las consultas por equipo y por problema |
| `V2__agregar_status_a_submissions.sql` | Agrega `status` en tres pasos (columna nullable → relleno del histórico → `NOT NULL`), su `CHECK` contra `SubmissionStatus` y el índice parcial que alimenta el reintento de encolamiento |

Detalles que conviene conocer antes de desplegar sobre una base de datos ya existente:

- **Relleno del histórico:** los envíos anteriores a `V2` nunca pasaron por el encolamiento, así que su estado se deduce del veredicto — `verdict = 'PENDING'` → `status = 'PENDING'`, cualquier otro veredicto → `status = 'JUDGED'`. Las filas que queden en `PENDING` serán reencoladas por el barrido de reintentos en el primer arranque, que es justo lo que se quiere: son envíos que nunca llegaron al juez.
- **Base de datos compartida:** todos los microservicios comparten la misma BD, por eso el servicio lleva su propio historial (`spring.flyway.table=flyway_schema_history_submission`) y toma como línea base la versión `0` (`spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=0`). Sin `baseline-version=0`, Flyway daría `V1` por aplicada solo porque el esquema no está vacío por culpa de otro servicio.
- **Idempotencia:** `V1` y `V2` usan `IF NOT EXISTS` para tolerar entornos donde la tabla ya la creó `ddl-auto` en su día.

> 📖 El detalle completo de la mensajería —garantías de entrega, colas de mensajes
> muertos, reintentos y contrato con `judge-service`— está en
> [`services/submission-service/docs/RABBITMQ.md`](./services/submission-service/docs/RABBITMQ.md).

> ⚠️ **Importante:** `submission-service` **no juzga** el código. Solo orquesta la entrada. El juicio es responsabilidad de `judge-service`.

---

### `judge-service` — Puerto `8084`
**Responsabilidad:** Motor de evaluación (Judging Engine). Consume la cola de RabbitMQ, crea contenedores Docker aislados, ejecuta el código contra los casos de prueba y emite el veredicto.

| Elemento de dominio | Descripción |
|---------------------|-------------|
| `JudgeTask`         | submissionId, language, sourceCode, testCases, timeLimit, memoryLimit. Se ensambla con `JudgeTask.create(...)` y valida sus invariantes (al menos un caso de prueba, límites en rango) |
| `JudgeResult`       | submissionId, verdict, executionTimeMs, memoryUsedKb, failedTestCase (`null` si es `ACCEPTED` o si falló antes de ejecutar, p. ej. compilación). Se crea con `JudgeResult.create(...)` |
| `TestCase`          | VO propio del juez: id, input, expectedOutput, orderIndex. No comparte modelo con `problem-service` |
| Límites del sandbox | VOs validados: `TimeLimit`, `MemoryLimit`, `PidsLimit`, `VolumeSizeLimit`, `OutputSizeLimit`, `ErrorSizeLimit`, `HardTimePercent`, `WatchIntervalMillis`, `AbsoluteTimeLimit` |

**Puertos de dominio y excepciones:**
- `TestCaseRepository` (`domain/repository`) — lectura de los casos de prueba de un problema; los datos pertenecen a `problem-service`.
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
| `config/` | Convertidor JSON de RabbitMQ, bean del `Runner` y `RestClient` hacia `problem-service` |

**Imagen del sandbox (`docker/`):** `judge-service` es el único servicio que **no se ejecuta con `./gradlew bootRun`** para evaluar de verdad: necesita `bubblewrap`, el filtro seccomp y una rama de cgroups delegada. Su [`Dockerfile`](./services/judge-service/docker/Dockerfile) compila el jar y lo empaqueta sobre Ubuntu con `bubblewrap` y `python3`, `gen_seccomp.py` genera el filtro en el build, y `entrypoint.sh` prepara `/cg` al arrancar. El contenedor se declara en [`infrastructure/docker/docker-compose.yml`](../infrastructure/docker/docker-compose.yml) bajo el perfil `sandbox`, de modo que un `docker compose up` normal siga levantando solo PostgreSQL y RabbitMQ:

```bash
sudo cp infrastructure/systemd/goslint.slice /etc/systemd/system/ && sudo systemctl enable --now goslint.slice
JWT_SECRET=<mínimo 32 bytes> docker compose --profile sandbox up -d --build judge-service
```

**Endpoints de `judge-service`:**

| Método | Endpoint | Acceso | Notas |
|--------|----------|--------|-------|
| GET    | `/api/v1/judge/monitor-limits` | ADMIN | Límites vigentes de los monitores |
| PUT    | `/api/v1/judge/monitor-limits` | ADMIN | Reemplaza los cinco límites: `outputSizeBytes`, `errorSizeBytes`, `hardTimePercent`, `watchIntervalMs`, `absoluteTimeMs`. Un valor fuera de rango o ausente responde `400` |
| GET    | `/actuator/health` | Público | Estado del servicio, sin detalles |

Los límites (`OutputHandle` usa `outputSizeBytes`, `ErrorsHandle` usa `errorSizeBytes` y `TimeWatchdog` usa los otros tres) arrancan con `app.sandbox.monitor.*`. El `Runner` los lee al empezar cada evaluación, así que un cambio solo aplica a las que empiecen después, y **se pierde al reiniciar**: no hay base de datos donde persistirlos.

> 📖 El detalle del sandbox —el comando de bwrap y el filtro seccomp, los límites y
> métricas de cgroups, y lo que hay que preparar en el host— está en
> [`BWRAP.md`](./services/judge-service/docs/BWRAP.md),
> [`CGROUPS.md`](./services/judge-service/docs/CGROUPS.md) y
> [`REQUIREMENTS.md`](./services/judge-service/docs/REQUIREMENTS.md).

`judge-service` **no declara topología**: exchange, colas y DLQ las declara `submission-service`, dueño del contrato (ver su [`RABBITMQ.md`](./services/submission-service/docs/RABBITMQ.md)). No usa base de datos; se excluye la autoconfiguración de JPA que el `build.gradle` raíz aplica a todos los módulos.

**Veredictos emitidos:** `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `MEMORY_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR`

> 🔒 **Seguridad del Sandbox:** El contenedor Docker corre con:
> - Red deshabilitada (`--network none`)
> - Sistema de archivos de solo lectura excepto `/tmp`
> - Límite de PIDs para evitar fork bombs
> - `--memory` y `--cpus` configurados por problema

**Lenguajes soportados:** C, C++, Java, Python

---

### `feedback-service` — Puerto `8085`
**Responsabilidad:** Generar retroalimentación educativa inteligente usando un LLM externo (OpenAI / Gemini) tras un veredicto de evaluación.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Feedback`         | id, submissionId, aiSuggestion, generatedAt |

**Casos de uso principales:**
- `GenerateFeedbackUseCase` — Construye prompt con (código + veredicto + métricas) y llama a la API LLM
- `GetFeedbackUseCase` — Recupera retroalimentación generada

**Restricciones:**
- El LLM **no debe revelar** la solución del problema.
- Solo se genera feedback cuando el veredicto **no** es `ACCEPTED`.
- Depende del EIF externo (API LLM) — su disponibilidad está sujeta al proveedor.

**Tablas BD:** `feedback`

---

### `contest-service` — Puerto `8086`
**Responsabilidad:** Creación y gestión de maratones de programación (concursos), equipos y scoreboard.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Contest`          | id, createdBy, password, title, startTime, endTime, status, description |
| `Team`             | id, contestId, score, penaltyTime, nameTeam |
| `Member`           | teamId, userId, registeredAt |
| `ContestProblem`   | contestId, problemId, points, orderIndex, alias |

**Casos de uso principales:**
- `CreateContestUseCase` — Solo ORGANIZER/ADMIN
- `RegisterTeamUseCase` — Inscripción de equipos con contraseña
- `GetScoreboardUseCase` — Ranking en tiempo real (potencialmente via WebSocket/SSE)
- `AddProblemToContestUseCase` — Asignación de problemas al concurso

**Tablas BD:** `contests`, `teams`, `members`, `contest_problems`

---

## 6. Seguridad y autorización

La seguridad se implementa con **Spring Security** y **JWT** de forma stateless. Cada microservicio hereda la dependencia `spring-boot-starter-security` desde `shared/common-infrastructure` y define su propia configuración de seguridad.

### 6.1 Modelo de roles

Los roles actuales del sistema son:

| Rol         | Tipo de actor | Permisos principales |
|-------------|---------------|----------------------|
| `ADMIN`     | Usuario humano | Acceso total a problemas, concursos y usuarios. |
| `ORGANIZER` | Usuario humano | Gestión de problemas y concursos. |
| `STUDENT`   | Usuario humano | Lectura pública de problemas, envío de soluciones y consulta de propios envíos. |
| `SERVICE`   | Microservicio  | Rol técnico para comunicación **servicio-a-servicio**. No representa un usuario humano. |

### 6.2 Restricción de endpoints con `@PreAuthorize`

Los controladores REST usan la anotación `@PreAuthorize` de Spring Security para declarar los roles permitidos en cada método:

```java
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")
@PostMapping("/{problemId}")
public ResponseEntity<TestCaseResponseDTO> create(...) { ... }
```

Spring Security evalúa esta expresión comparando las **authorities** del `Authentication` actual. `hasAnyRole('ADMIN','ORGANIZER','SERVICE')` busca las authorities `ROLE_ADMIN`, `ROLE_ORGANIZER` o `ROLE_SERVICE` en el contexto de seguridad.

### 6.3 El rol `SERVICE` y los endpoints GET de test cases

El rol `SERVICE` existe para permitir que otros microservicios consuman datos protegidos sin necesidad de un usuario humano autenticado. **No se usa en operaciones de escritura**: crear, actualizar o eliminar problemas y test cases solo pueden hacerlo usuarios humanos con rol `ADMIN` u `ORGANIZER`.

#### Caso de uso: `judge-service` necesita leer los test cases

Cuando `judge-service` evalúa una solución enviada por un estudiante, debe ejecutar el código contra **todos** los casos de prueba del problema, incluyendo los casos privados que los estudiantes no deben ver. Para ello, `judge-service` se autenticará con un JWT propio que contenga el claim:

```json
{
  "role": "SERVICE"
}
```

Este JWT será validado por el futuro filtro de seguridad de `problem-service`, que convertirá el claim `role=SERVICE` en la authority `ROLE_SERVICE`. Por eso, los únicos endpoints de `problem-service` que aceptan el rol `SERVICE` son los de **lectura** de test cases:

```
GET /api/v1/problems/test-cases/{problemId}/all
GET /api/v1/problems/test-cases/{id}
```

Ambos están restringidos a `@PreAuthorize("hasAnyRole('ADMIN','ORGANIZER','SERVICE')")`, permitiendo que `judge-service` (y solo él, con el JWT correcto) recupere los test cases necesarios para la evaluación.

#### Test cases de ejemplo (samples): endpoint público

Los estudiantes necesitan ver al menos algunos casos de prueba para entender el problema. El campo `isSample` de `TestCase` marca cuáles son públicos. El endpoint:

```
GET /api/v1/problems/test-cases/{problemId}/samples
```

**no lleva `@PreAuthorize`** y es accesible sin autenticación. Retorna únicamente los casos donde `isSample = true`, filtrados desde la base de datos mediante el caso de uso `GetAllSampleTestCasesByProblemIdUseCase`.

### 6.4 Estado actual de la seguridad

- **JWT aún no está implementado**: por ahora `problem-service` tiene una configuración de seguridad mínima (`SecurityConfig`) que habilita `@EnableMethodSecurity` y permite todas las requests a nivel de filtro HTTP (`anyRequest().permitAll()`).
- **Las restricciones por rol ya están escritas** en los controllers y se activarán completamente cuando se agregue el filtro JWT que extraiga el rol del token y lo convierta a `ROLE_*`.
- El rol `SERVICE` **no está en el enum `Role` del `auth-service`** porque no es un rol de usuario humano. Cuando se implemente la generación de JWTs para microservicios, el claim `role=SERVICE` se mapeará directamente a la authority `ROLE_SERVICE`.
- **El canal WebSocket ya autentica de verdad.** `JwtTokenValidator` (en `shared/common-infrastructure`) verifica firma, emisor y vigencia, y `JwtHandshakeInterceptor` lo aplica durante el handshake: una conexión sin token válido se rechaza con `401` y nunca llega a abrirse. El mismo bean servirá al futuro filtro JWT de los endpoints HTTP, de modo que ambos lados validen igual.
- **La emisión de tokens sigue pendiente**: `auth-service` solo expone `/register`. El WebSocket únicamente consume un token ya emitido; el login es una historia aparte y siempre HTTP.

### 6.5 Canal de notificación en tiempo real

`submission-service` expone un WebSocket en `/ws/submissions` para empujar los cambios de estado de los envíos.

| Aspecto | Decisión |
|---------|----------|
| Sentido | **Unidireccional.** El servidor empuja; lo que el cliente envíe se descarta sin interpretarlo. No hay comandos, ni suscripciones, ni STOMP. |
| Autenticación | JWT validado en el handshake, en este orden: subprotocolo `bearer.<token>`, cabecera `Authorization: Bearer`, o query param `token`. |
| Alcance | El servidor asocia la conexión al `sub` del token y solo entrega envíos cuyo equipo dueño incluye a ese usuario. |
| Destinatarios | Se resuelven vía `TeamMembershipPort`. Mientras `contest-service` no exista, `NoOpTeamMembershipAdapter` trata cada equipo como individual. |

Los navegadores no permiten fijar cabeceras al abrir un WebSocket, de ahí el subprotocolo. El cliente anuncia dos y el servidor confirma solo el fijo:

```js
new WebSocket("wss://.../ws/submissions", ["goslint-judge", `bearer.${token}`]);
```

Mensaje emitido (`SubmissionStatusEventDTO`):

```json
{
  "type": "SUBMISSION_STATUS_UPDATED",
  "submissionId": "…", "problemId": "…", "teamId": "…",
  "status": "JUDGED", "verdict": "ACCEPTED",
  "executionTimeMs": 120, "memoryUsedKb": 2048,
  "occurredAt": "2026-08-25T18:30:00Z"
}
```

No incluye el código fuente: ya está en poder de quien lo envió y no tiene por qué recorrer la red otra vez.

> 📖 El detalle del canal —las tres vías de las que se extrae el token, el registro
> de sesiones, cómo se resuelven los destinatarios y las limitaciones conocidas al
> escalar— está en
> [`services/submission-service/docs/WEBSOCKET.md`](./services/submission-service/docs/WEBSOCKET.md).

## 7. Flujo de datos principal (envío de código)

```
[Estudiante]
    │  POST /submissions  (HTTP)
    ▼
[submission-service]
    │  1. Valida JWT con auth-service
    │  2. Persiste Submission (status: PENDING, verdict: PENDING)
    │  3. Publica → RabbitMQ: exchange "submission.exchange",
    │     routing key "submission.evaluate" → cola "submission.evaluate"
    │  4. Con la confirmación del broker, marca la Submission como QUEUED
    │     (si no llega, la fila sigue en PENDING y el barrido la reintenta)
    │
    ▼
[judge-service]  ← Consumidor de "submission.evaluate"
    │  5. Recupera test cases desde problem-service
    │  6. Compila el código en contenedor Docker
    │  7. Ejecuta vs. cada test case (sandbox)
    │  8. Determina veredicto
    │  9. Publica → RabbitMQ: exchange "submission.exchange",
    │     routing key "submission.judged" → cola "submission.judged"
    │
    ├─▶ [submission-service] ← Consumidor de "submission.judged"
    │     10. Actualiza Submission: veredicto + executionTimeMs + memoryUsedKb
    │         (status: JUDGED)
    │     11. Tras el commit, empuja el nuevo estado por WebSocket al dueño
    │         del envío → la pantalla se actualiza sin recargar ni hacer polling
    │
    └─▶ [feedback-service] ← Si no es ACCEPTED:
           12. Llama API LLM con prompt
           13. Persiste Feedback
```

> Si el envío no puede completar su recorrido —el juez agota los reintentos, o el
> veredicto no consigue registrarse— el mensaje acaba en la cola de mensajes
> muertos correspondiente (`submission.evaluate.dlq` / `submission.judged.dlq`).
> `ExhaustedSubmissionDeadLetterListener` lo recoge y cierra el envío con estado
> `SYSTEM_ERROR`, que se notifica por el mismo WebSocket. Sin ese cierre el envío
> se quedaría para siempre aparentando estar en cola y el estudiante esperando un
> veredicto que nadie va a emitir.

> El envío **nunca se pierde por un fallo de mensajería**: si el paso 3 no obtiene
> confirmación del broker, la fila se queda en `PENDING` y
> `PendingSubmissionRetryScheduler` la vuelve a encolar periódicamente. Por eso el
> estado se persiste en la tabla `submissions` y no solo en memoria.

---

## 7. Convenciones de paquetes Java

El prefijo base de todos los paquetes es `co.uceva.<servicio>`.

```
co.uceva.auth/
├── domain/
│   ├── model/          ← Clases de entidad (User.java)
│   ├── valueobject/    ← Value Objects (Email.java, Role.java)
│   └── exception/      ← Excepciones de dominio (UserAlreadyExistsException.java)
├── application/
│   ├── port/
│   │   ├── in/         ← Interfaces de casos de uso (RegisterUserUseCase.java)
│   │   └── out/        ← Interfaces de repositorios/puertos externos (UserRepository.java)
│   └── service/        ← Implementaciones de casos de uso (AuthApplicationService.java)
└── infrastructure/
    ├── web/            ← @RestController
    │   ├── dto/        ← Request/Response DTOs (RegisterRequest.java)
    │   └── mapper/     ← Conversión dominio ↔ DTO
    ├── persistence/    ← @Repository, JPA entities (@Entity), Spring Data Repos
    └── config/         ← @Configuration (SecurityConfig, RabbitConfig, etc.)
```

### Reglas de nomenclatura

| Tipo de clase          | Sufijo recomendado       | Ejemplo                          |
|------------------------|--------------------------|----------------------------------|
| Caso de uso (interfaz) | `UseCase`                | `SubmitCodeUseCase`              |
| Servicio de aplicación | `ApplicationService`     | `SubmissionApplicationService`   |
| Puerto de salida       | `Repository` / `Port`    | `SubmissionRepository`           |
| Controlador REST       | `Controller`             | `SubmissionController`           |
| JPA Entity             | `JpaEntity`              | `SubmissionJpaEntity`            |
| Adaptador de repo      | `RepositoryAdapter`      | `SubmissionRepositoryAdapter`    |
| DTO de request         | `Request`                | `SubmitCodeRequest`              |
| DTO de response        | `Response`               | `SubmissionResponse`             |
| Mapper                 | `Mapper`                 | `SubmissionMapper`               |

---

## 8. Configuración de Gradle

### `build.gradle` raíz
Gestiona el BOM de Spring Boot 3.2.4 y dependencias comunes a todos los sub-proyectos:
- `org.projectlombok:lombok:1.18.30` (compileOnly + annotationProcessor)
- `spring-boot-starter-test`

### `settings.gradle`
Declara los 8 sub-proyectos:
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

### `build.gradle` de cada microservicio
Cada servicio debe:
1. Aplicar el plugin `org.springframework.boot` para generar el JAR ejecutable.
2. Declarar `implementation project(':shared:common-infrastructure')`.
3. Agregar sus dependencias específicas (JPA, RabbitMQ, Docker client, etc.).

**Ejemplo para `submission-service`:**
```gradle
plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':shared:common-infrastructure')
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'   // RabbitMQ
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
}
```

> Todos los servicios aplican ya el plugin. Los que siguen siendo esqueleto (`feedback`, `contest`; `judge` está a medias) aún no declaran sus dependencias reales (Docker client, cliente HTTP del LLM, etc.): se añaden al implementarlos.

### Gradle Wrapper
Ya está en el repositorio (`gradlew`, `gradle/wrapper/`). Se usa `./gradlew` desde `backend/`; no hace falta Gradle instalado globalmente.

---

## 9. Problemas conocidos y deuda técnica

| # | Problema | Criticidad | Acción requerida |
|---|----------|-----------|------------------|
| 1 | **`auth-service` no emite JWT**: solo expone `/register` | 🔴 Alta | Sin login no hay tokens que validar. Es lo que bloquea el filtro JWT de todos los servicios y obliga al bypass temporal de `submission-service` |
| 2 | **Ningún endpoint HTTP valida el JWT**: los `@PreAuthorize` están escritos pero la autenticación llega anónima | 🔴 Alta | Escribir el filtro JWT reutilizando `JwtTokenValidator` (ya usado por el handshake del WebSocket) y retirar `TemporaryAuthBypassFilter` |
| 3 | **`problem-service` arranca con `ddl-auto=validate` pero su carpeta `db/migration/` está vacía** | 🔴 Alta | Nadie crea sus tablas: el arranque contra una BD limpia falla. Escribir su migración base como se hizo en `submission-service` |
| 4 | **`auth-service` sigue con `ddl-auto=update` y Flyway desactivado** | 🟡 Media | El esquema de `users` no está versionado; migrar a Flyway + `validate` para que deje de depender de lo que Hibernate decida en cada arranque |
| 5 | **El sandbox nunca se ha ejecutado sobre cgroups reales** | 🔴 Alta | La imagen construye y el filtro seccomp pasa sus 19 comprobaciones, pero falta levantarla sobre una `goslint.slice` de verdad y correr `prueba_humo.sh`. Hasta entonces, la evaluación solo está probada con mocks |
| 5 | **`judge-service` solo evalúa Python** | 🟡 Media | El `Compiler` (C, C++, Java) va en otra HU; hasta entonces esos envíos acaban en `SYSTEM_ERROR` tras agotar los reintentos |
| 6 | **`contest-service` no existe**: la composición real de los equipos se desconoce | 🟡 Media | `NoOpTeamMembershipAdapter` trata cada equipo como individual. Al llegar el servicio, añadir un adaptador y cambiar `app.team-membership.provider` |
| 7 | **El registro de sesiones WebSocket es local a la instancia** | 🟡 Media | Con varias réplicas, la instancia que recibe el veredicto puede no tener la conexión del estudiante. Escalar horizontalmente exige compartir el registro (p. ej. Redis) |
| 8 | **`traefik.yml` está vacío** | 🟡 Media | Sin API Gateway cada servicio se expone por su puerto. Al configurarlo, cuidar el reenvío de `Upgrade`/`Sec-WebSocket-Protocol` o el handshake del WebSocket no se completa |
| 9 | **`app.websocket.allowed-origins=*`** | 🟡 Media | Cómodo en desarrollo; restringir a los orígenes del frontend antes de exponer el servicio |
| 10 | **Test package incorrecto** en `auth-service` y `problem-service` | 🟢 Baja | La clase de test usa `co.uceva.judge.auth_service` en lugar de `co.uceva.auth` |
| 11 | **`services/users-service/` conviven con `auth-service`** | 🟢 Baja | Hay dos módulos para la misma responsabilidad; decidir cuál queda y retirar el otro de `settings.gradle` |

---

## 10. Cómo agregar un nuevo microservicio

> ⚠️ Si en el futuro el sistema necesita escalar con un nuevo servicio (ej. `notification-service`), seguir estos pasos:

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
   ```

---

## 11. Checklist antes de hacer un PR

Antes de enviar cambios en cualquier microservicio, verificar:

- [ ] Las entidades en `domain/` **no importan** ninguna clase de Spring Boot, JPA o cualquier framework.
- [ ] Los casos de uso en `application/` **no importan** clases de `infrastructure/`.
- [ ] Los controladores REST están en `infrastructure/web/` y usan DTOs, **no** entidades de dominio directamente.
- [ ] Las implementaciones de repositorios JPA están en `infrastructure/persistence/`.
- [ ] Se creó una migración de Flyway en `src/main/resources/db/migration/` para cualquier cambio de esquema.
- [ ] El nombre del paquete de test coincide con el paquete del código fuente (`co.uceva.<servicio>`).
- [ ] El `build.gradle` del servicio tiene el plugin `org.springframework.boot` aplicado.
- [ ] Se añadió la dependencia correcta en `settings.gradle` si se creó un nuevo módulo.

---

*Documento generado el 2026-06-10; última actualización el 2026-09-05. Actualizar cuando cambien decisiones arquitectónicas.*
