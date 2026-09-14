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

> ⚠️ **Importante:** `submission-service` **no juzga** el código. Solo orquesta la entrada. El juicio es responsabilidad de `judge-service`.

---

### `judge-service` — Puerto `8084`
**Responsabilidad:** Motor de evaluación (Judging Engine). Consume la cola de RabbitMQ, crea contenedores Docker aislados, ejecuta el código contra los casos de prueba y emite el veredicto.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `JudgeTask`        | submissionId, language, sourceCode, testCases, timeLimitMs, memoryLimitKb |
| `JudgeResult`      | submissionId, verdict, executionTimeMs, memoryUsedKb, failedTestCase |

**Casos de uso principales:**
- `EvaluateSubmissionUseCase` — Orquesta el pipeline de compilación y ejecución
- `RunInSandboxUseCase` — Lanza contenedor Docker, aplica límites de CPU/RAM, captura stdout/stderr
- `CompareOutputUseCase` — I/O Matching contra el expected output del test case

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
    │  9. Publica → RabbitMQ: queue "submission.judged"
    │
    ├─▶ [submission-service] ← Actualiza Submission con veredicto (status: JUDGED)
    │
    └─▶ [feedback-service] ← Si no es ACCEPTED:
           10. Llama API LLM con prompt
           11. Persiste Feedback
           12. Notifica al cliente (WebSocket / polling)
```

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

> ⚠️ **Problema actual:** Ningún microservicio tiene `id 'org.springframework.boot'` en su `build.gradle` propio ni las dependencias reales (JPA, RabbitMQ, etc.). Deben añadirse cuando se empiece a implementar cada servicio.

### Gradle Wrapper (pendiente)
El proyecto no tiene Gradle Wrapper inicializado. Para agregarlo:
1. Instala Gradle (≥ 8.x): https://gradle.org/install/
2. Desde la carpeta `backend/`: `gradle wrapper --gradle-version 8.9`
3. Commitea los archivos generados: `gradlew`, `gradlew.bat`, `gradle/wrapper/`

---

## 9. Problemas conocidos y deuda técnica

| # | Problema | Criticidad | Acción requerida |
|---|----------|-----------|------------------|
| 1 | **No hay Gradle Wrapper** en el repositorio | 🔴 Alta | Inicializar con `gradle wrapper` |
| 2 | **Microservicios sin plugin `org.springframework.boot`** en su `build.gradle` individual | 🔴 Alta | Cada servicio necesita este plugin para ser ejecutable |
| 3 | **Test package incorrecto** en `auth-service` y `problem-service` | 🟡 Media | La clase de test usa `co.uceva.judge.auth_service` en lugar de `co.uceva.auth` |
| 4 | **`application.properties` vacíos** — solo tienen `spring.application.name` | 🟡 Media | Agregar puerto, configuración de BD, etc. cuando se implementen |
| 5 | **Sin `application.properties`** en servicios nuevos (submission, judge, feedback, contest) | 🟡 Media | Crear el archivo con al menos `spring.application.name` y el puerto |
| 6 | **`shared/common-domain` y `shared/common-infrastructure`** sin código fuente | 🟡 Media | Crear la estructura de carpetas `src/main/java/co/uceva/shared/` |
| 7 | **`docker-compose.yml` y `traefik.yml` vacíos** | 🟡 Media | Completar con la configuración de infraestructura |
| 8 | **Sin Gradle Wrapper**, el CI/CD no puede construir el proyecto | 🔴 Alta | Resolver el punto 1 primero |
| 9 | **`problem-service` arranca con `ddl-auto=validate` pero su carpeta `db/migration/` está vacía** | 🔴 Alta | Nadie crea sus tablas: el arranque contra una BD limpia falla. Escribir su migración base como se hizo en `submission-service` |
| 10 | **`auth-service` sigue con `ddl-auto=update` y Flyway desactivado** | 🟡 Media | El esquema de `users` no está versionado; migrar a Flyway + `validate` para que el esquema deje de depender de lo que Hibernate decida en cada arranque |

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

*Documento generado el 2026-06-10. Actualizar cuando cambien decisiones arquitectónicas.*
