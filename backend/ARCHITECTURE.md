# Goslint Judge — Guía de Arquitectura del Backend

> **Trabajo de Grado** | Sistema de Evaluación Automática de Código Fuente para Maratones de Programación – UCEVA  
> **Stack**: Java 17 · Spring Boot 3.2.4 · Gradle (Multi-project) · PostgreSQL · RabbitMQ · Redis · Docker  
> **Patrón**: Monolito modular (`contest-platform`: módulos `auth`, `problem`, `contest` y `submission`; el módulo `feedback` es solo un esqueleto de carpetas) + `judge-service` desacoplado por RabbitMQ. Arquitectura Limpia dentro de los módulos `problem`, `contest`, `submission` y `judge-service`; `auth` usa capas planas

---

## Tabla de Contenido

1. [Visión general del sistema](#1-visión-general-del-sistema)
2. [Mapa del monorepo](#2-mapa-del-monorepo)
3. [Arquitectura Limpia — La regla de dependencia](#3-arquitectura-limpia--la-regla-de-dependencia)
4. [Módulos compartidos (shared)](#4-módulos-compartidos-shared)
5. [Microservicios — Responsabilidades y dominio](#5-microservicios--responsabilidades-y-dominio)
   - [Estado de implementación](#estado-de-implementación)
   - [Modelo de datos](#modelo-de-datos)
   - [Configuración y variables de entorno](#configuración-y-variables-de-entorno)
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
| Redis          | Solo lo usa el módulo `auth`: intentos fallidos, bloqueos de cuentas, refresh tokens, lista negra de JWT |
| bubblewrap + cgroups v2 | Sandbox de ejecución de código dentro del contenedor del `judge-service` (solo Linux) |
| API LLM        | Retroalimentación inteligente (pendiente)                       |
| Traefik        | API Gateway / reverse proxy (`infrastructure/traefik/traefik.yml` está vacío) |
| Spring Mail (SMTP) | Correo de invitaciones a equipos                            |
| Spring WebSocket | Notificación de estado de los envíos                          |
| JWT (JJWT 0.12.5) | Autenticación stateless entre clientes y servicios           |
| Flyway         | Migraciones de esquema de base de datos                         |

---

## 2. Mapa del monorepo

```
backend/
├── build.gradle                  ← BOM Spring Boot 3.2.4, Lombok, JaCoCo, tests comunes
├── settings.gradle               ← Declara 2 módulos shared y 2 servicios
├── gradle.properties             ← Variables globales
├── .env.example                  ← Plantilla de variables para correr con Gradle (el `.env` real no se commitea)
├── docker/spring-service.Dockerfile ← Imagen genérica para servicios Spring (argumento SERVICE)
├── scripts/                      ← start-services.sh · stop-services.sh · start-judge.sh
│

├── shared/                       ← Librerías internas reutilizables
│   ├── common-domain/            ← Enums y eventos que cruzan más de un servicio
│   │   └── build.gradle          ← Sin dependencias externas; solo Lombok
│   └── common-infrastructure/    ← Seguridad base (JWT) reutilizable por todos los servicios
│       └── build.gradle          ← api: spring-web, spring-security, spring-data-redis, jjwt
│
└── services/
    ├── contest-platform/         ← Puerto 8080. Monolito modular (paquete co.uceva.contests_platform)
    │   ├── auth/                 ←   registro, login, perfil, administración y búsqueda de usuarios (capas planas)
    │   ├── problem/              ←   problemas y casos de prueba
    │   ├── contest/              ←   maratones, equipos, invitaciones, aclaraciones, scoreboard
    │   ├── submission/           ←   envíos, RabbitMQ, WebSocket
    │   ├── feedback/             ←   retroalimentación con LLM (esqueleto: solo carpetas con .gitkeep)
    │   └── docs/                 ←   AUTH_API.md · RABBITMQ.md · WEBSOCKET.md
    └── judge-service/            ← Puerto 8084. Sandbox (bwrap + cgroups v2), sin BD
        ├── docker/               ←   Dockerfile · entrypoint.sh · gen_seccomp.py · prueba_humo.sh
        └── docs/                 ←   BWRAP.md · CGROUPS.md · REQUIREMENTS.md · WORKERS.md
```

> `auth-service`, `problem-service`, `submission-service`, `contest-service` y `feedback-service` **ya no existen como servicios**: se integraron en `contest-platform` (rama `refactor/migrar-la-arquitectura-del-sistema-a-un-monolito-modular`). Los módulos se llaman entre sí por inyección de casos de uso, sin HTTP.

Estructura interna de `contest-platform` (resumen por módulo, bajo `src/main/java/co/uceva/contests_platform/`):

```
contests_platform/
├── ContestsPlatformApplication.java
├── auth/                     ← capas planas
│   ├── controller/           ←   AuthController · AdminUserController · UserSearchController · AuthExceptionHandler · BearerTokens
│   ├── service/              ←   AuthService · ProfileService · AdminUserService · SearchUsersByUsernameUseCase · GetUserByIdUseCase
│   ├── repository/ model/ dto/ exception/
│   ├── security/             ←   JwtService
│   ├── cache/                ←   AuthCache (Redis, implementa TokenBlacklist)
│   └── config/               ←   SecurityConfig · JwtAuthenticationFilter · SuperAdminInitializer
├── problem/                  ← domain/{model,exception,repository,valueobject} · application/usecase{,/impl} · infrastructure/{mapper,persistence,web}
├── contest/                  ← domain/{model,exception,repository,service,valueobject} · application/usecase{,/impl}
│                               infrastructure/{client,config,email,mapper,persistence,web}
├── submission/               ← domain/{model,exception,repository,service} · application/{event,exception,port/out,usecase{,/impl}}
│                               infrastructure/{client,config,mapper,messaging,notification,persistence,web,websocket}
└── feedback/                 ← solo carpetas vacías (.gitkeep)
```

`judge-service` (paquete `co.uceva.judge`) usa las tres capas:

```
services/judge-service/
├── build.gradle
├── docker/                   ← imagen del sandbox
└── src/main/
    ├── java/co/uceva/judge/
    │   ├── domain/           ← model · valueobject · repository · exception
    │   ├── application/      ← usecase{,/impl} · port/out · exception
    │   └── infrastructure/   ← client · config · mapper · messaging · persistence · sandbox · security · web
    └── resources/application.properties   (sin migraciones: no tiene BD)
```

`contest-platform` guarda su `application.properties` y las migraciones Flyway en `src/main/resources/` (`db/migration/V1`–`V11`).

---

## 3. Arquitectura Limpia — La regla de dependencia

Los módulos `problem`, `contest` y `submission` de `contest-platform` y el `judge-service` siguen la **Clean Architecture** de Robert C. Martin (el módulo `auth` usa capas planas, ver §5). La regla fundamental es:

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
| `TokenBlacklist` | Interfaz con `isBlacklisted(token)`. `JwtTokenValidator` la consulta solo si se le pasa en su constructor de tres argumentos `(secret, issuer, tokenBlacklist)`. La implementa `AuthCache` (módulo `auth`). |

> **Nota:** ninguna clase de `common-infrastructure` usa Redis directamente: la dependencia `spring-boot-starter-data-redis` la aprovecha `AuthCache` en `contest-platform`. El `JwtTokenValidator` que registra `SecurityConfig` en `contest-platform` y en `judge-service` se construye sin lista negra (constructor de dos argumentos), de modo que el filtro JWT no consulta Redis (ver §6.3).

---

## 5. Microservicios — Responsabilidades y dominio

### Estado de implementación

| Servicio / módulo | Puerto | Estado | Qué hay hoy |
|-------------------|--------|--------|-------------|
| `contest-platform` · módulo `auth` | 8080 | 🟢 Funcional | Registro, login, logout, refresh con rotación, perfil (`/me`, cambio de username y de contraseña), administración de usuarios y super administrador inicial. Emite JWT (Access 15 min y Refresh 8 h, distinguidos por el claim `type`). Bloqueo de cuenta por intentos fallidos y lista negra de tokens en Redis, búsqueda de usuarios por username para invitar a equipos. Tabla `users` creada por Flyway `V7` |
| `contest-platform` · módulo `problem` | 8080 | 🟢 Funcional | CRUD de problemas y casos de prueba, restricciones por rol con `@PreAuthorize`, endpoint público de *samples*. Tablas por Flyway `V11` |
| `contest-platform` · módulo `contest` | 8080 | 🟢 Funcional | Maratones con contraseña de inscripción y cambio automático de estado por programador de tareas, problemas de maratón (puntos, penalización, globo), equipos de 1 a 3 integrantes con capitán, invitaciones por correo (SMTP), aclaraciones y scoreboard (ordenado por puntaje y penalización; el puntaje no se actualiza con los veredictos, ver §11). Flyway `V1`–`V6` |
| `contest-platform` · módulo `submission` | 8080 | 🟢 Funcional | Recepción del envío con regla de elegibilidad (integrante del equipo, concurso activo, problema del concurso), encolamiento en RabbitMQ con confirmación del broker, consumo del veredicto, cierre por error del sistema desde las DLQ, reintento de pendientes y notificación en tiempo real por WebSocket. Flyway `V8`–`V10` |
| `judge-service` | 8084 | 🟢 Funcional | Consume `submission.evaluate`, avisa el inicio en `submission.judging`, obtiene casos de prueba y límites del módulo `problem` por HTTP, compila y ejecuta en el sandbox (`bwrap`/cgroups) con un pool de workers (`JUDGE_WORKERS`, 2 por defecto) y publica el veredicto en `submission.judged`. Evalúa **Python, C, C++ y Java**. El JWT `SERVICE` hacia `contest-platform` lo firma el propio juez con el secreto compartido. Validado end-to-end en Manjaro x86_64 y en Ubuntu OCI A1-flex aarch64 (según `docs/REQUIREMENTS.md`). Solo corre en Linux: se despliega en su propia imagen con `bubblewrap` y seccomp (perfil `sandbox` del compose) |
| `contest-platform` · módulo `feedback` | 8080 | 🔴 Esqueleto | Solo la estructura de carpetas vacía (`application`, `domain`, `infrastructure`, con archivos `.gitkeep`); sin clases, sin endpoints, sin tablas |

**Documentación detallada por subsistema:**

| Documento | Contenido |
|-----------|-----------|
| [`services/contest-platform/docs/AUTH_API.md`](./services/contest-platform/docs/AUTH_API.md) | Contrato de la API de autenticación, perfil y administración de usuarios |
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
| `SUPER_ADMIN` | Dueño de la plataforma. Único que asigna y retira administradores. Se crea al arrancar `contest-platform` con `SUPER_ADMIN_EMAIL`/`SUPER_ADMIN_PASSWORD`; no se obtiene por registro ni por la API (`SuperAdminInitializer`; sin esas variables no se crea). Los `@PreAuthorize` de `problem`, `submission` y del juez lo incluyen junto a `ADMIN`; los de `contest` (maratones, problemas de maratón, equipos, aclaraciones) listan solo `ADMIN` y `ORGANIZER` (ver §11). |
| `ADMIN` | Administrador. Gestiona estudiantes y organizadores (no a otros administradores). |
| `ORGANIZER` | Organizador. Gestiona problemas y concursos. |
| `STUDENT` | Estudiante. Rol que recibe todo registro público (`User.newStudent`). Lectura pública, equipos, aclaraciones y envíos. |
| *`SERVICE`* | Rol técnico. **No está en el enum** `Role` del módulo `auth` porque no es un usuario humano. `judge-service` lo firma directamente en sus tokens vía `ServiceTokenProvider`. |

Jerarquía de gestión de usuarios: `SUPER_ADMIN` > `ADMIN` > `ORGANIZER` > `STUDENT`. Un `ADMIN` solo puede cambiar el rol o el estado de `STUDENT` y `ORGANIZER`; solo el `SUPER_ADMIN` asigna o retira `ADMIN`; el rol `SUPER_ADMIN` no se puede asignar y su cuenta no se puede modificar; nadie cambia su propio rol y nadie se desactiva a sí mismo.

**Estructura:** capas planas (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `security`, `cache`, `config`), sin interfaces de casos de uso ni adaptadores. Detalle en [`services/contest-platform/docs/AUTH_API.md`](./services/contest-platform/docs/AUTH_API.md).

| Clase | Responsabilidad |
|-------|-----------------|
| `AuthService` | Registro (email en minúsculas, unicidad sin distinguir mayúsculas), login con bloqueo por intentos fallidos, logout (lista negra + borrado del refresh token) y refresh con rotación |
| `ProfileService` | Consulta de perfil, cambio de username y de contraseña (esta última cierra todas las sesiones) y disponibilidad de username |
| `AdminUserService` | Listado paginado con búsqueda, cambio de rol y de estado, con reglas de jerarquía (el rol del actor se lee de la base de datos) |
| `JwtService` | Emite y lee JWT (JJWT 0.12.5, `jwt.secret` / `jwt.issuer`). Cada token lleva `type` = `access` o `refresh` |
| `AuthCache` | Redis: `auth:failed_attempts:*`, `auth:lock:*`, `auth:refresh_token:*`, `auth:blacklist:<sha256>`. Implementa `TokenBlacklist` |
| `SearchUsersByUsernameUseCase` | Búsqueda de hasta 20 usuarios activos cuyo username contiene el texto, ordenados por username; excluye a quien busca y a los roles `ADMIN`, `SUPER_ADMIN` y `ORGANIZER` |
| `SuperAdminInitializer` | Al arrancar crea el super administrador si no existe ninguno y están definidas `SUPER_ADMIN_EMAIL` y `SUPER_ADMIN_PASSWORD` |

**Endpoints activos:**

| Método | Endpoint | Acceso | Request Body | Respuesta |
|--------|----------|--------|-------------|----------|
| POST | `/api/v1/auth/register` | Público | `{firstName, lastName, username, email, password, institution?}` | 201 `{message, userId}` |
| POST | `/api/v1/auth/login` | Público | `{email, password}` | 200 `{accessToken, refreshToken, expiresIn, type: "Bearer"}` |
| POST | `/api/v1/auth/refresh` | Público | `{refreshToken}` | 200 igual que login |
| POST | `/api/v1/auth/logout` | Público (usa el Bearer si viene) | — | 200 siempre |
| GET | `/api/v1/auth/me` | Bearer | — | 200 perfil |
| PATCH | `/api/v1/auth/me/username` | Bearer | `{username}` | 200 perfil · 409 en uso |
| PATCH | `/api/v1/auth/me/password` | Bearer | `{currentPassword, newPassword}` | 204 · 422 contraseña actual incorrecta |
| GET | `/api/v1/auth/check-username?username=` | Público | — | 200 `{available}` · 400 formato inválido |
| GET | `/api/v1/users/search?q=` | Autenticado | — | 200 lista `{id, username}` (máx. 20; no incluye correo ni rol) |
| GET | `/api/v1/admin/users?page&size&search` | ADMIN / SUPER_ADMIN | — | 200 página de usuarios |
| PATCH | `/api/v1/admin/users/{id}/role` | ADMIN / SUPER_ADMIN | `{role}` | 200 usuario · 403 jerarquía · 404 |
| PATCH | `/api/v1/admin/users/{id}/status` | ADMIN / SUPER_ADMIN | `{isActive}` | 200 usuario · 403 (incl. autodesactivación) · 404 |

**Validaciones:**
- `firstName`, `lastName`: obligatorios, 2–50 caracteres.
- `username`: 3–30 caracteres (letras, dígitos, `_`, `.`).
- `email`: obligatorio, formato válido, máximo 255 caracteres.
- `password` / `newPassword`: mínimo 8 caracteres, al menos 1 mayúscula, 1 número y 1 carácter especial.
- `institution`: opcional, máximo 255 caracteres.

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
| `Problem` | `id`, `createdBy`, `title`, `statement`, `timeLimitMs`, `memoryLimitKb`, `difficult (value object Difficulty)`, `createdAt`, `inputFormat`, `outputFormat` |
| `TestCase` | `id`, `problemId`, `expectedOutput`, `orderIndex`, `isSample`, `input`, `output`, `createdAt` |

**Value objects:**

| Value Object | Validación |
|-------------|-----------|
| `Difficulty` | Entero entre **800 y 3500**, múltiplo de 100 (escala tipo Codeforces) |
| `TimeLimit` | Entre 100 y 5 000 ms |
| `MemoryLimit` | Entre 4 096 KB (4 MB) y 262 144 KB (256 MB) |

**Casos de uso implementados:**

| Caso de uso | Descripción |
|-------------|-------------|
| `CreateProblemUseCase` | Crea un problema; `createdBy` viaja en el cuerpo de la petición (`CreateProblemRequestDTO`), no se toma del JWT |
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
| POST | `/api/v1/problems` | ADMIN, SUPER_ADMIN, ORGANIZER | Crear problema |
| GET | `/api/v1/problems/{id}` | Público | Obtener problema por ID (incluye el enunciado) |
| GET | `/api/v1/problems/all` | Público | Listar todos los problemas |
| GET | `/api/v1/problems/title/{title}` | Público | Buscar por título |
| PUT | `/api/v1/problems/{id}` | ADMIN, SUPER_ADMIN, ORGANIZER | Actualizar problema |
| DELETE | `/api/v1/problems/{id}` | ADMIN, SUPER_ADMIN, ORGANIZER | Eliminar problema |
| POST | `/api/v1/problems/test-cases/{problemId}` | ADMIN, SUPER_ADMIN, ORGANIZER | Crear test case |
| POST | `/api/v1/problems/test-cases/{problemId}/batch` | ADMIN, SUPER_ADMIN, ORGANIZER | Crear test cases en lote |
| POST | `/api/v1/problems/test-cases/batch-delete` | ADMIN, SUPER_ADMIN, ORGANIZER | Eliminar test cases en lote |
| GET | `/api/v1/problems/test-cases/{problemId}/all` | ADMIN, SUPER_ADMIN, ORGANIZER, SERVICE | Listar todos los test cases (incluye privados) |
| GET | `/api/v1/problems/test-cases/{problemId}/samples` | Público | Listar solo los test cases de ejemplo |
| GET | `/api/v1/problems/test-cases/{id}` | ADMIN, SUPER_ADMIN, ORGANIZER, SERVICE | Obtener test case por ID |
| PUT | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN, SUPER_ADMIN, ORGANIZER | Actualizar test case |
| PUT | `/api/v1/problems/test-cases/{problemId}/reorder` | ADMIN, SUPER_ADMIN, ORGANIZER | Reordenar test cases |
| DELETE | `/api/v1/problems/test-cases/{problemId}/{testCaseId}` | ADMIN, SUPER_ADMIN, ORGANIZER | Eliminar test case |

> El rol `SERVICE` en los endpoints de test cases permite a `judge-service` leer los casos de prueba privados durante la evaluación.

**Seguridad:** la de `contest-platform` (ver §6): un único `JwtAuthenticationFilter` y `@EnableMethodSecurity`. Los `@PreAuthorize` aceptan `ADMIN` y `SUPER_ADMIN` juntos.

**BD:** Tablas `problems` y `test_cases`, creadas por la migración `V11__Create_problems_and_test_cases_tables.sql`.

---

### Módulo `submission` (`contest-platform`, puerto `8080`)
**Responsabilidad:** Recibir envíos de código, validar su elegibilidad, persistirlos y orquestar el ciclo de evaluación vía mensajería. Paquete `co.uceva.contests_platform.submission`.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Submission` | `id`, `teamId`, `problemId`, `language (ProgrammingLanguage)`, `sourceCode (SourceCode)`, `verdict (VerdictStatus)`, `status (SubmissionStatus)`, `executionTimeMs`, `memoryUsedKb`, `codeSizeBytes`, `submittedAt` |

**Casos de uso implementados (13 en total):**
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
| POST | `/api/v1/submissions` | STUDENT, ADMIN, SUPER_ADMIN, ORGANIZER | Enviar solución. Cuerpo: `{teamId, problemId, language, sourceCode}` → 201 |
| GET | `/api/v1/submissions/{id}` | STUDENT, ADMIN, SUPER_ADMIN, ORGANIZER, SERVICE | Obtener envío por ID (incluye el código fuente) |
| GET | `/api/v1/submissions` | ADMIN, SUPER_ADMIN, ORGANIZER | Listar todos los envíos |
| GET | `/api/v1/submissions/problem/{problemId}` | ADMIN, SUPER_ADMIN, ORGANIZER, SERVICE | Envíos de un problema |
| GET | `/api/v1/submissions/team/{teamId}` | STUDENT, ADMIN, SUPER_ADMIN, ORGANIZER | Envíos de un equipo |
| GET | `/api/v1/submissions/history` | STUDENT, ADMIN, SUPER_ADMIN, ORGANIZER | Historial equipo+problema (`?problemId=&teamId=`) |
| GET | `/api/v1/submissions/{id}/metrics` | STUDENT, ADMIN, SUPER_ADMIN, ORGANIZER, SERVICE | Métricas de un envío |
| DELETE | `/api/v1/submissions/{id}` | ADMIN, SUPER_ADMIN, ORGANIZER | Eliminar envío |

Solo `POST /api/v1/submissions` valida la pertenencia del usuario al equipo (elegibilidad). Las consultas `GET` exigen rol pero no comprueban que el envío o el equipo consultado pertenezcan al usuario autenticado (ver §11).

**BD:** Tabla `submissions`, con tres migraciones Flyway dentro de la numeración global del servicio:  
- `V8` — Tabla base con check constraints de `language` y `verdict`  
- `V9` — Columna `status` con ciclo de vida `PENDING/QUEUED/JUDGING/JUDGED`  
- `V10` — Amplió el check constraint de `status` para incluir `ENQUEUE_ERROR`  
La restricción `ck_submissions_verdict` creada en `V8` enumera siete veredictos y no incluye `JUDGE_ERROR` (ver §11). `JPA_DDL_AUTO` vale `update` por defecto (ver §11). Historial Flyway: `flyway_schema_history_contest`, compartido por todos los módulos.

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

**Puertos de salida (`application/port/out`):** `ProblemLimitsPort` (límites de tiempo y memoria del problema), `SandboxExecutor` (ejecuta la tarea y devuelve el `JudgeResult`), `JudgeResultPublisher` (publica el resultado hacia `contest-platform`) y `SubmissionJudgingNotifier` (avisa que el envío entró a evaluación, `submission.judging`).

**Casos de uso de administración del sandbox:** `GetMonitorLimitsUseCase` y `UpdateMonitorLimitsUseCase`.

**Lenguajes y comandos** (`CompilationCommandFactory` / `RunCommandFactory`):

| Lenguaje | Compilación | Ejecución |
|----------|-------------|-----------|
| `C` | `gcc -std=c17 ... -lm` | `/solution/solution` |
| `CPP` | `g++ -std=gnu++20` | `/solution/solution` |
| `JAVA` | `javac -encoding UTF-8` | `java -Dfile.encoding=UTF-8 -cp /solution <Clase>` |
| `PYTHON` | `python3 -m py_compile` | `python3 /solution/solution.py` |

Si la compilación falla, el resultado es `COMPILATION_ERROR` sin ejecutar casos de prueba. La compilación corre en el contenedor del juez; la ejecución, dentro de `bwrap`.

**Veredictos que puede emitir:** `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `MEMORY_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR` y `JUDGE_ERROR` (fallo del propio sandbox, no del estudiante; ver `docs/WORKERS.md`).

**Infraestructura (`infrastructure/`):**

| Pieza | Rol |
|-------|-----|
| `messaging/SubmissionEvaluationListener` | Consume `submission.evaluate`. Un fallo transitorio se propaga para el reintento (3 intentos, luego DLQ); uno permanente (`TestCasesNotFoundException`, `IllegalArgumentException`) se rechaza sin reintentar |
| `messaging/RabbitSubmissionJudgingNotifierAdapter` | Publica `SubmissionJudgingStartedEvent` con routing key `submission.judging` |
| `messaging/RabbitJudgeResultPublisherAdapter` | Publica `SubmissionJudgedEvent` en `submission.exchange` con routing key `submission.judged`, persistente y esperando la confirmación del broker |
| `security/ServiceTokenProvider` | Emite el JWT `role=SERVICE` del juez (HS256, secreto y emisor compartidos, `sub` = `app.security.jwt.service-id`, vigencia 5 min) y lo reutiliza hasta 30 s antes de expirar. Exige `JWT_SECRET` (mín. 32 bytes, sin valor por defecto) |
| `client/ProblemServiceTestCaseRepositoryAdapter` | `TestCaseRepository` sobre `GET /api/v1/problems/test-cases/{id}/all` (JWT `SERVICE` en cada petición); un `404` o lista vacía es `TestCasesNotFoundException` |
| `client/ProblemServiceLimitsAdapter` | `ProblemLimitsPort` sobre `GET /api/v1/problems/{id}` |
| `sandbox/RunnerSandboxExecutor` | `SandboxExecutor` sobre `Runner`: escribe el fuente a un temporal, ejecuta y traduce el mapa de resultados a `JudgeResult` |
| `web/MonitorLimitsController` | `GET`/`PUT /api/v1/judge/monitor-limits`: consulta y modifica los límites de los monitores del sandbox (`ADMIN` o `SUPER_ADMIN`) |
| `sandbox/worker/JudgeWorkerPool` · `JudgeWorker` · `WorkerEnvironment` | Pool de evaluaciones en paralelo (`app.sandbox.workers.count`): cada worker tiene su hilo, su rama de cgroups (`<cgroup-root>/worker-<n>`) y su directorio (`<work-root>/worker-<n>`); un worker que falla o supera el tiempo máximo (`evaluation-timeout-seconds`, 300 s) se reinicia |
| `security/JwtAuthenticationFilter` + `config/SecurityConfig` | Autentica el HTTP con el JWT y el mismo `JwtTokenValidator` que el resto; el rol pasa a `ROLE_<rol>` para `@PreAuthorize` |
| `persistence/InMemoryMonitorLimitsRepository` | Guarda los límites vigentes en memoria (el juez no tiene BD) |
| `config/` | Convertidor JSON de RabbitMQ, bean del `Runner` y `RestClient` hacia `contest-platform` (`app.problem-service.base-url`, variable `PROBLEM_SERVICE_URL`) |

**Componentes del sandbox:**
- `BwrapCommandFactory` — construye el comando `bwrap` con namespaces, bind mounts y límites de cgroups
- `Runner` — lanza el proceso en el sandbox, captura stdout/stderr con monitores
- `Compiler` — compila el código antes de ejecutar (C, C++, Java y Python)
- `TestCaseRunner` — ejecuta cada caso de prueba y decide el veredicto
- `RunnerSandboxExecutor` — adaptador (`TaskEvaluator`) que orquesta `Compiler` + `Runner` y traduce el resultado a `JudgeResult`
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

**Seguridad:** `JwtAuthenticationFilter` activo. `judge-service` tiene su propia `SecurityConfig` con `anyRequest().authenticated()`: todas las rutas exigen un JWT válido (sin excepción explícita para `/actuator/health`, el único endpoint de actuator expuesto).

**Ejecución:** el juez solo funciona en Linux (kernel 5.19 o superior por `memory.peak`, cgroups v2, `bubblewrap`, `seccomp`). No corre en Docker Desktop de Windows/macOS. Requisitos del host en `docs/REQUIREMENTS.md`.

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
| `Contest`          | `id`, `createdBy`, `title`, `description`, `passwordHash` (BCrypt), `period (ContestPeriod: startTime/endTime)`, `status (UPCOMING/ACTIVE/ENDED)`, `createdAt` |
| `Team`             | `id`, `contestId`, `captainId`, `teamName (TeamName)`, `teamScore (score, penaltyTime)`, `createdAt`, integrantes (`members`) |
| `Member`           | `teamId`, `userId`, `registeredAt` |
| `Invitation`       | `id`, `teamId`, `invitedUserId`, `invitedEmail`, `status (PENDING/ACCEPTED/REJECTED/EXPIRED)`, `createdAt`, `respondedAt` |
| `ContestProblem`   | `id`, `contestId`, `problemId`, `alias`, `orderIndex`, `points`, `penalty`, `balloonColor` |
| `Clarification`    | `id`, `problemId` (**id del `ContestProblem`**, no del problema del catálogo), `teamId`, `question`, `answer`, `status (PENDING/ANSWERED)`, `createdAt`, `answeredAt` |

**Reglas de negocio visibles en el código:**

| Regla | Detalle |
|-------|---------|
| Ventana del concurso | `endTime` debe ser posterior a `startTime`; al crear el concurso, o al cambiar su inicio, `startTime` no puede estar en el pasado. Se crea en estado `UPCOMING` |
| Cambio de estado | `ContestSchedulerService` programa (en memoria, hilos `contest-status-*`, pool de 2) el paso a `ACTIVE` en `startTime` y a `ENDED` en `endTime`. Se reprograma al crear o actualizar un concurso y, al arrancar, para todos los no terminados (`ContestSchedulerInitializer`) |
| Inscripción | Unirse es crear un equipo: `POST /contests/{contestId}/teams` con `{teamName, contestPassword}`. Solo antes de `startTime` (después responde `RegistrationClosedException`) y con la contraseña del concurso correcta |
| Equipo | Nombre de 3 a 50 caracteres, único dentro del concurso. Un usuario solo puede estar en un equipo por concurso. El creador queda como capitán e integrante. Entre 1 y 3 integrantes (`Team.DEFAULT_MAX_MEMBERS = 3`) |
| Invitaciones | Solo las crea el capitán, con las inscripciones abiertas, equipo no lleno, invitado sin equipo en ese concurso y sin otra invitación pendiente para el mismo equipo. Se envía un correo (SMTP) con enlaces a `/api/v1/invitations/{id}/respond?action=accept` o `reject` más `token`; el token es un HMAC-SHA256 del id de la invitación firmado con el secreto JWT. Solo el invitado puede aceptar o rechazar; si al aceptar el equipo ya está lleno, la invitación pasa a `EXPIRED` |
| Problemas del concurso | Alias normalizado a mayúsculas, máximo 10 caracteres, único por concurso; un mismo problema no se repite en el concurso; `orderIndex` ≥ 0; `points` > 0 (1 por defecto); `penalty` ≥ 0, minutos por intento fallido (20 por defecto); `balloonColor` en formato `#RRGGBB`. El problema debe existir en el módulo `problem` |
| Aclaraciones | La pregunta cualquier usuario autenticado que pertenezca a un equipo del concurso del problema; máximo 2 000 caracteres (pregunta y respuesta). Cada una se responde una sola vez |
| Scoreboard | Equipos del concurso ordenados por `score` descendente y `penaltyTime` ascendente. Ver §11: ningún flujo actualiza esos campos hoy |

**Endpoints activos** (prefijo `/api/v1`):

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/contests` | ADMIN, ORGANIZER | Crear maratón `{createdBy, title, description, password, startTime, endTime}` |
| GET | `/contests` | Público | Listar maratones |
| GET | `/contests/{id}` | Público | Obtener maratón (la respuesta incluye `status`; no expone la contraseña) |
| GET | `/contests/title/{title}` | Público | Buscar por título |
| GET | `/contests/creator/{createdBy}` | Público | Maratones de un creador |
| PUT | `/contests/{id}` | ADMIN, ORGANIZER | Actualizar maratón (una contraseña vacía conserva la actual) |
| DELETE | `/contests/{id}` | ADMIN, ORGANIZER | Eliminar maratón |
| POST | `/contests/{contestId}/problems` | ADMIN, ORGANIZER | Asignar un problema `{problemId, alias, orderIndex, points?, penalty?, balloonColor}` |
| GET | `/contests/{contestId}/problems` | Público | Problemas del maratón por `orderIndex` |
| PUT | `/contest-problems/{id}` | ADMIN, ORGANIZER | Editar alias, posición, puntos, penalización y color |
| DELETE | `/contest-problems/{id}` | ADMIN, ORGANIZER | Retirar el problema del maratón |
| POST | `/contests/{contestId}/teams` | Autenticado | Crear equipo e inscribirse `{teamName, contestPassword}` |
| GET | `/contests/{contestId}/teams` | Público | Equipos del maratón (`?name=` filtra por nombre) |
| GET | `/contests/{contestId}/scoreboard` | Público | Equipos ordenados por puntaje y penalización |
| GET | `/teams`, `/teams/{id}`, `/teams/user/{userId}`, `/teams/{id}/members` | Público | Consultas de equipos e integrantes |
| PUT | `/teams/{id}` | Autenticado | Renombrar equipo |
| DELETE | `/teams/{id}` | ADMIN, ORGANIZER | Eliminar equipo |
| POST | `/teams/{id}/members` | ADMIN, ORGANIZER | Agregar integrante directamente |
| DELETE | `/teams/{id}/members/{userId}` | Autenticado | Quitar integrante |
| POST | `/teams/{teamId}/invitations` | Autenticado (capitán) | Invitar a un usuario `{userId}` |
| GET | `/invitations/me` | Autenticado | Invitaciones del usuario |
| POST | `/invitations/{id}/accept`, `/invitations/{id}/reject` | Autenticado (invitado) | Responder una invitación (aceptar devuelve el equipo) |
| GET / POST | `/invitations/{id}/respond` | Público (con token del correo) | GET muestra la confirmación en HTML; POST aplica la respuesta |
| POST | `/contest-problems/{problemId}/clarifications` | Autenticado | Preguntar `{question}`; `{problemId}` es el id del `ContestProblem` |
| GET | `/contests/{contestId}/clarifications` | Público | Aclaraciones del maratón |
| POST | `/clarifications/{id}/answer` | ADMIN, ORGANIZER | Responder una aclaración |

> No hay endpoints propios para iniciar o terminar un maratón ni para consultar solo su estado: el `status` va en la respuesta del maratón y lo cambian el programador de tareas o un `PUT /contests/{id}`.

**Casos de uso** (`contest/application/usecase`): Create/Update/Delete/Get de concursos (por id, título y creador), `StartContest`, `FinishContest`; Create/Update/Delete/Get de equipos (por id, concurso, nombre y usuario); `AddMember`, `RemoveMember`, `GetMembersByTeam`; `AddProblemToContest`, `UpdateContestProblem`, `RemoveProblemFromContest`, `GetProblemsByContest`; `InviteMember`, `AcceptInvitation`, `RejectInvitation`, `GetInvitationById`, `GetInvitationsByUser`; `AskClarification`, `AnswerClarification`, `GetClarificationsByContest`; `GetScoreboard`.

**Infraestructura propia:** `SmtpEmailSender` e `InvitationLinks` (correo de invitación; usan `MAIL_*`, `APP_PUBLIC_URL` y `FRONTEND_URL`), `ProblemServiceCatalogAdapter` (comprueba que un problema exista al asignarlo a un maratón, llamando al caso de uso del módulo `problem` in-process), `UserRepositoryAdapter` (correo del usuario invitado, desde el módulo `auth`), `ContestPasswordEncoderConfig` (BCrypt) y `ContestSchedulerService` / `ContestSchedulerInitializer` / `SchedulerAutoUpdateConfig` (cambio de estado programado).

**Integración con `submission`:** el módulo `submission` lee equipos, horarios del concurso y problemas del concurso llamando directamente a `GetTeamByIdUseCase`, `GetContestByIdUseCase` y `GetProblemsByContestUseCase`.

**Tablas BD:** `contests`, `teams`, `team_members`, `team_invitations`, `contest_problems`, `clarifications` (Flyway `V1`–`V6`).

---

### Modelo de datos

Un único esquema PostgreSQL (`goslint_judge_db`) compartido por todos los módulos. Las migraciones `V1`–`V11` están en `contest-platform/src/main/resources/db/migration/` y usan una numeración global (no sigue el orden de los módulos).

| Tabla | Módulo | Migración | Columnas principales |
|-------|--------|-----------|----------------------|
| `users` | auth | `V7` | `id`, `first_name`, `last_name`, `username` (único), `email` (único), `password_hash`, `role`, `institution`, `is_active`, `created_at` |
| `problems` | problem | `V11` | `id`, `created_by`, `title`, `statement`, `time_limit_ms`, `memory_limit_kb`, `difficulty`, `input_format`, `output_format`, `created_at` |
| `test_cases` | problem | `V11` | `id`, `problem_id`, `expected_output`, `order_index`, `is_sample`, `input`, `output`, `created_at` |
| `contests` | contest | `V1` | `id`, `created_by`, `title`, `description`, `password_hash`, `start_time`, `end_time`, `status`, `created_at` |
| `teams` | contest | `V2`, `V3` | `id`, `contest_id`, `captain_id`, `team_name` (único por concurso), `score`, `penalty_time`, `created_at` |
| `team_members` | contest | `V2` | `id`, `team_id`, `user_id` (único por equipo), `registered_at` |
| `team_invitations` | contest | `V3` | `id`, `team_id`, `invited_user_id`, `invited_email`, `status`, `created_at`, `responded_at` (índice único parcial: una invitación `PENDING` por equipo y usuario) |
| `contest_problems` | contest | `V4`, `V5` | `id`, `contest_id`, `problem_id`, `alias`, `order_index`, `points`, `penalty`, `balloon_color` |
| `clarifications` | contest | `V6` | `id`, `problem_id` (→ `contest_problems.id`), `team_id`, `question`, `answer`, `status`, `created_at`, `answered_at` |
| `submissions` | submission | `V8`–`V10` | `id`, `team_id`, `problem_id`, `language`, `source_code`, `verdict`, `status`, `execution_time_ms`, `memory_used_kb`, `code_size_bytes`, `submitted_at` |

**Relaciones:**

```
contests 1 ──< teams 1 ──< team_members
   │             ├────────< team_invitations
   │             └────────< clarifications >── 1 contest_problems
   └────────────< contest_problems
problems 1 ──< test_cases
```

- Con clave foránea (y `ON DELETE CASCADE`): `teams.contest_id`, `team_members.team_id`, `team_invitations.team_id`, `contest_problems.contest_id`, `clarifications.problem_id` y `.team_id`, `test_cases.problem_id`.
- Referencias lógicas **sin** clave foránea: `contests.created_by`, `problems.created_by`, `teams.captain_id`, `team_members.user_id`, `team_invitations.invited_user_id` → `users.id`; `contest_problems.problem_id` → `problems.id`; `submissions.team_id` → `teams.id` y `submissions.problem_id` → `problems.id`.
- Flyway usa la tabla de historial `flyway_schema_history_contest` con `baseline-on-migrate=true` y `baseline-version=0`.

---

### Configuración y variables de entorno

`contest-platform` se configura en `src/main/resources/application.properties` (no hay perfiles `application-*.yml`; solo `application-local.properties` ignorado por git). Variables principales (el valor tras `:` es el predeterminado):

| Variable | Predeterminado | Uso |
|----------|----------------|-----|
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `DB_URL` · `DB_USERNAME` · `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/goslint_judge_db` · `postgres` · (sin valor) | PostgreSQL |
| `JWT_SECRET` | **obligatoria** (mín. 32 bytes) | Clave HMAC. `app.security.jwt.secret` no tiene valor por defecto; sin la variable el servicio no arranca |
| `JWT_ISSUER` | `goslint-judge` | Emisor de los tokens |
| `JWT_EXPIRATION_ACCESS` · `JWT_EXPIRATION_REFRESH` | `900000` (15 min) · `28800000` (8 h) | Vigencia en ms |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://192.168.18.51:3000,http://127.0.2.2:3000` | Orígenes permitidos por CORS (separados por coma) |
| `SUPER_ADMIN_EMAIL` · `SUPER_ADMIN_PASSWORD` · `SUPER_ADMIN_USERNAME` | vacío · vacío · `superadmin` | Super administrador inicial |
| `REDIS_HOST` · `REDIS_PORT` | `localhost` · `6379` | Redis (módulo `auth`) |
| `RABBITMQ_HOST` · `RABBITMQ_PORT` · `RABBITMQ_USERNAME` · `RABBITMQ_PASSWORD` | `localhost` · `5672` · `guest` · `guest` | RabbitMQ |
| `MAIL_HOST` · `MAIL_PORT` · `MAIL_USERNAME` · `MAIL_PASSWORD` · `MAIL_SMTP_AUTH` · `MAIL_STARTTLS` · `MAIL_FROM` | `localhost` · `25` · vacío · vacío · `false` · `false` · `no-reply@goslint.local` | Correo de invitaciones |
| `APP_PUBLIC_URL` · `FRONTEND_URL` | `http://localhost:8080` · `http://localhost:3000` | URLs para los enlaces del correo |
| `FLYWAY_ENABLED` · `FLYWAY_TABLE` · `FLYWAY_BASELINE_ON_MIGRATE` | `true` · `flyway_schema_history_contest` · `true` | Flyway |
| `JPA_DDL_AUTO` · `JPA_DIALECT` · `JPA_SHOW_SQL` | `update` · `PostgreSQLDialect` · `false` | Hibernate |

Las claves `app.messaging.*` (exchange, colas, routing keys y DLQ), `app.submission.retry.*` y `app.websocket.*` van fijas en `application.properties`; `app.messaging.enabled=true` activa el publicador RabbitMQ (con `false`, los envíos se guardan en `PENDING` y no se publican). `app.websocket.allowed-origins` vale `*`.

`judge-service` (`application.properties`): `JWT_SECRET` (obligatoria, igual que en `contest-platform`), `JWT_ISSUER`, `JUDGE_SERVICE_ID`, `RABBITMQ_*`, `PROBLEM_SERVICE_URL` (`http://localhost:8080`), `JUDGE_WORKERS` (`2`), `JUDGE_EVALUATION_TIMEOUT_SECONDS` (`300`), `CG_DIR` (`/cg`), `WORK_DIR` (`/work`) y `JUDGE_SANDBOX_LOG_LEVEL` (`INFO`).

---

## 6. Seguridad y autorización

### 6.1 Modelo de roles

| Rol | Tipo de actor | Permisos principales |
|-----|---------------|----------------------|
| `SUPER_ADMIN` | Usuario humano | Gestión de administradores y de todos los usuarios. Acepta `problem`, `submission`, `/api/v1/admin/**` y el juez; en los endpoints de `contest` no figura en `@PreAuthorize` (ver §11). |
| `ADMIN` | Usuario humano | Administración de usuarios (estudiantes y organizadores), problemas, concursos y envíos. |
| `ORGANIZER` | Usuario humano | Gestión de problemas, concursos y aclaraciones. |
| `STUDENT` | Usuario humano | Lectura pública, equipos, aclaraciones y envío de soluciones. |
| `SERVICE` | Microservicio | Rol técnico S2S. `judge-service` lo usa para leer test cases privados y límites del módulo `problem`. No existe en el enum `Role` del módulo `auth`. |

### 6.2 Patrón de seguridad implementado (`contest-platform` y `judge-service`)

Ambos servicios implementan el mismo patrón de filtro JWT, con una diferencia en la cadena (ver punto 3):

1. **`JwtTokenValidator`** (bean): `contest-platform` lo declara en `auth/config/SecurityConfig`; `judge-service` en su propia configuración. Lee `app.security.jwt.secret` y `app.security.jwt.issuer`. La clave **no tiene valor por defecto** (`JWT_SECRET`): un secreto heredado en producción aceptaría tokens falsificados. Si falta la variable de entorno, el servicio no arranca.

2. **`JwtAuthenticationFilter`** (extiende `OncePerRequestFilter`): Extrae el header `Authorization: Bearer <token>`, llama a `JwtTokenValidator.validate()`, convierte el claim `role` en la authority `ROLE_<rol>` y lo publica en `SecurityContextHolder`. Si el token es inválido o está ausente, la petición **continúa como anónima** — el filtro no la rechaza.

3. **`SecurityConfig`** (`@Configuration @EnableWebSecurity @EnableMethodSecurity`): Registra el filtro antes de `UsernamePasswordAuthenticationFilter`. Configura la cadena como stateless y sin CSRF. En `contest-platform` añade CORS (`CORS_ALLOWED_ORIGINS`; métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`; cabeceras `Content-Type` y `Authorization`; credenciales permitidas) y establece `anyRequest().permitAll()`: la autorización real la imponen los `@PreAuthorize` en cada método del controlador. Los controladores de `contest`, `problem` y `submission` llevan además `@CrossOrigin(origins = "*")`. En `judge-service` la cadena usa `anyRequest().authenticated()` y responde `401` sin credenciales.

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

El módulo `auth` emite los tokens, así que `/api/v1/auth/login`, `/register`, `/refresh` y `/check-username` son públicos. Los endpoints de perfil (`/me/**`) validan el access token que reciben (firma, emisor, tipo `access`, lista negra y cuenta activa). Los refresh tokens llevan `type=refresh` y `JwtTokenValidator` los rechaza como credencial de acceso. Como `auth` ya comparte proceso con el resto de módulos, la validación de los demás endpoints es la del filtro único de §6.2. Ese filtro usa un `JwtTokenValidator` sin lista negra, así que un access token revocado con `logout` (o por cambio de contraseña) solo se rechaza en los endpoints del módulo `auth` que consultan `AuthCache` (`/me/**`, `/admin/**`); en los demás sigue siendo válido hasta que expire (15 min).

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
| Orígenes permitidos | `app.websocket.allowed-origins` = `*` (`setAllowedOriginPatterns`), fijo en `application.properties` |
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
```

---

## 8. Flujo de datos principal (envío de código)

```
[Cliente/Estudiante]
    │  POST /api/v1/submissions  (HTTP, JWT requerido)
    ▼
[contest-platform :8080]
    │  1. JwtAuthenticationFilter valida el JWT
    │  2. @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN','ORGANIZER')")
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
    │  11. Compila (C, C++, Java, Python) y ejecuta cada caso; determina el veredicto
    │  12. Publica SubmissionJudgedEvent → RabbitMQ (routing: submission.judged)
    │
    ├─▶ [contest-platform] ← Consumidor de "submission.judged"
    │     13. UpdateSubmissionVerdictUseCase: actualiza veredicto + métricas (status: JUDGED)
    │     14. NotifySubmissionStatusUseCase: empuja estado por WebSocket a los integrantes del equipo
    │     (no se actualiza el puntaje del equipo: el flujo termina aquí, ver §11)
    │
    └─▶ [contest-platform, módulo feedback] ← PENDIENTE DE IMPLEMENTAR
           15. Llamada API LLM (si veredicto ≠ ACCEPTED)
           16. Persistir Feedback
```

**Mecanismos de resiliencia:**
- **DLQ:** Mensajes rechazados en `submission.evaluate.dlq`, `submission.judging.dlq` o `submission.judged.dlq` → `ExhaustedSubmissionDeadLetterListener` cierra el envío con `ENQUEUE_ERROR` y notifica por WebSocket.
- **Reintento:** `PendingSubmissionRetryScheduler` barre filas en `PENDING` periódicamente y las reencola.

---

## 9. Convenciones de paquetes Java

El prefijo base es `co.uceva.contests_platform.<módulo>` en `contest-platform` (`auth`, `problem`, `contest`, `submission`) `co.uceva.judge` en `judge-service` (y `co.uceva.shared` en los módulos `shared`).

> **El módulo `auth`** no sigue la estructura de abajo: usa capas planas (`controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `security/`, `cache/`, `config/`). El esquema de puertos y adaptadores descrito a continuación aplica a los demás módulos y al juez, con variaciones (por ejemplo, `submission` agrupa sus puertos de salida en `application/port/out` y `contest` y `problem` los dejan en `domain/repository` y `domain/service`).

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

Hechos técnicos verificados contra el código actual, agrupados por área.

### Lógica de negocio y autorización

| # | Hecho | Criticidad | Acción posible |
|---|-------|-----------|----------------|
| 1 | **El puntaje del equipo no se actualiza al aceptar un veredicto.** `Team.addScore(...)` y `Team.updateScore(...)` existen pero ningún caso de uso los invoca; `UpdateSubmissionVerdictUseCaseImpl` solo persiste veredicto y métricas. `GET /contests/{id}/scoreboard` ordena por `score`/`penalty_time`, que se quedan en 0 | 🔴 Alta | Al recibir `ACCEPTED`, sumar los `points` del `ContestProblem` y la penalización de los intentos previos al equipo |
| 2 | **Consultas de envíos sin validar propiedad.** `GET /submissions/{id}`, `/team/{teamId}`, `/history` y `/{id}/metrics` aceptan el rol `STUDENT` pero no comprueban que el envío o el equipo pertenezca al usuario autenticado; la respuesta incluye el código fuente | 🔴 Alta | Comprobar la pertenencia al equipo en los casos de uso de consulta |
| 3 | **Equipos sin validar propiedad.** `PUT /teams/{id}` y `DELETE /teams/{id}/members/{userId}` solo exigen estar autenticado: no verifican que el usuario sea capitán o integrante del equipo | 🔴 Alta | Exigir capitán (o el propio integrante al salir) |
| 4 | **`SUPER_ADMIN` no figura en los `@PreAuthorize` del módulo `contest`** (maratones, problemas de maratón, equipos, aclaraciones: `hasAnyRole('ADMIN','ORGANIZER')`), mientras `problem`, `submission` y el juez sí lo incluyen | 🟡 Media | Unificar el criterio de roles |
| 5 | **`createdBy` llega en el cuerpo** de `POST /contests` y `POST /problems`, no del JWT: el cliente puede indicar otro autor | 🟡 Media | Tomar el autor de `AuthenticatedUser` |
| 6 | **Enunciados públicos.** `GET /problems/{id}`, `/problems/all`, `/problems/title/{title}` y `/problems/test-cases/{problemId}/samples` no exigen autenticación, aunque el problema pertenezca a un maratón en curso | 🟡 Media | Restringir el acceso mientras el problema esté en un maratón activo |
| 7 | **No hay endpoint para resolver nombres a partir de ids.** Solo existe `GET /users/search?q=` (por texto) y el listado de administración; las respuestas de equipos e invitaciones traen `userId`/`captainId` sin nombre. `GetUserByIdUseCase` existe pero no está expuesto | 🟡 Media | Añadir una consulta de usuarios por lote de ids |
| 8 | **El veredicto `JUDGE_ERROR` no está permitido por la base de datos.** El enum `VerdictStatus` lo incluye y el juez puede publicarlo, pero `ck_submissions_verdict` (migración `V8`) solo admite los otros siete valores y ninguna migración posterior lo amplía; al persistirlo se violaría la restricción y el mensaje acabaría en la DLQ | 🔴 Alta | Nueva migración que amplíe la restricción |

### Seguridad y configuración

| # | Hecho | Criticidad | Acción posible |
|---|-------|-----------|----------------|
| 9 | **Lista negra de tokens solo parcial.** El `JwtTokenValidator` de `contest-platform` se crea sin `TokenBlacklist`, así que el filtro JWT no consulta Redis: un access token revocado sigue siendo válido hasta expirar en los endpoints fuera de `auth` | 🟡 Media | Construir el validador con `AuthCache` |
| 10 | **`jwt.secret` conserva un valor por defecto de desarrollo** en `application.properties` (`${JWT_SECRET:dev-jwt-secret-...}`). `app.security.jwt.secret` exige `JWT_SECRET` y sin ella el servicio no arranca, pero existen dos juegos de propiedades (`jwt.*` para emitir, `app.security.jwt.*` para validar) | 🟡 Media | Unificar en `app.security.jwt.*` y quitar el valor por defecto |
| 11 | **`app.websocket.allowed-origins=*`** en `contest-platform`, y `@CrossOrigin(origins = "*")` en los controladores de `contest`, `problem` y `submission` (además del CORS global con `CORS_ALLOWED_ORIGINS`) | 🟡 Media | Restringir a los orígenes del frontend antes de exponer |
| 12 | **`JPA_DDL_AUTO` vale `update` por defecto** aunque Flyway gestiona el esquema | 🟡 Media | Pasar a `validate` |
| 13 | **`judge-service` exige JWT en todas sus rutas**, incluido `/actuator/health` (`anyRequest().authenticated()`): la comprobación de salud no es anónima | 🟢 Baja | Permitir `/actuator/health` en su `SecurityConfig` |

### Operación e infraestructura

| # | Hecho | Criticidad | Acción posible |
|---|-------|-----------|----------------|
| 14 | **`docker-compose.yml`, `backend/scripts/start-services.sh` y `stop-services.sh` siguen describiendo los antiguos servicios** `auth/problem/submission/contest-service` (puertos 8081–8086); no existe un servicio `contest-platform` en el compose. Además `judge-service` declara `depends_on: problem-service` y un `PROBLEM_SERVICE_URL` por defecto `http://problem-service:8082` | 🔴 Alta | Reemplazar esos servicios por `contest-platform` (8080) |
| 15 | **Registro de sesiones WebSocket local a la instancia** — no escala horizontalmente | 🟡 Media | Compartir el registro vía Redis cuando se requiera más de una réplica |
| 16 | **El programador de estados de concurso es en memoria** (hilos de una sola instancia); con varias réplicas se duplicaría | 🟡 Media | ShedLock/Quartz si se escala |
| 17 | **`traefik.yml` está vacío** — Sin API Gateway cada servicio se expone por su puerto | 🟡 Media | Configurar Traefik con reenvío de `Upgrade`/`Sec-WebSocket-Protocol` |
| 18 | **`MonitorLimits` de `judge-service` no persisten** — Los límites del sandbox se pierden en cada reinicio | 🟢 Baja | Persistencia (Redis o BD) si se quieren límites persistentes |
| 19 | **Módulo `feedback` es un esqueleto** — Toda su lógica está pendiente | 🔴 Alta | Implementar según el roadmap |
| 20 | **Clases de arranque de prueba con nombre del servicio viejo** (`ProblemServiceApplicationTests`, `SubmissionServiceApplicationTests` en `contest-platform`) | 🟢 Baja | Unificarlas en un único `ContestsPlatformApplicationTests` |

> Resueltos y ya no aplican: `/logout` en `auth`, Flyway para `users` (`V7`), soporte de C/C++/Java en el juez, `TokenBlacklist` en `JwtTokenValidator` (existe, ver #9 para su uso), residuos de `submission-service`/`users-service`, y los valores por defecto de `PROBLEM_SERVICE_URL`/`APP_PUBLIC_URL` (ahora `localhost:8080`).

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

*Documento actualizado el 2026-11-12 y verificado contra el código de `contest-platform`, `judge-service` y `shared`. Actualizar cuando cambien decisiones arquitectónicas.*
