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
- `CreateProblemUseCase` — Solo ADMIN/ORGANIZER
- `UploadTestCasesUseCase` — Carga de archivos de entrada/salida
- `GetProblemUseCase` — Lectura pública de enunciados

**Tablas BD:** `problems`, `test_cases`

---

### `submission-service` — Puerto `8083`
**Responsabilidad:** Recibir envíos de código de los estudiantes, persistirlos y publicar eventos a la cola de juicio.

| Entidad de dominio | Descripción |
|--------------------|-------------|
| `Submission`       | id, teamId, problemId, language, sourceCode, verdict, executionTimeMs, memoryUsedKb, codeSizeBytes, submittedAt |

**Casos de uso principales:**
- `SubmitCodeUseCase` — Valida, persiste y publica `SubmissionReceivedEvent` en RabbitMQ
- `GetSubmissionHistoryUseCase` — Historial por usuario/problema
- `GetSubmissionMetricsUseCase` — Tiempo y memoria de ejecución

**Tablas BD:** `submissions`

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

## 6. Flujo de datos principal (envío de código)

```
[Estudiante]
    │  POST /submissions  (HTTP)
    ▼
[submission-service]
    │  1. Valida JWT con auth-service
    │  2. Persiste Submission (estado: PENDING)
    │  3. Publica → RabbitMQ: queue "submission.evaluate"
    │
    ▼
[judge-service]  ← Consumidor de "submission.evaluate"
    │  4. Recupera test cases desde problem-service
    │  5. Compila el código en contenedor Docker
    │  6. Ejecuta vs. cada test case (sandbox)
    │  7. Determina veredicto
    │  8. Publica → RabbitMQ: queue "submission.judged"
    │
    ├─▶ [submission-service] ← Actualiza Submission con veredicto
    │
    └─▶ [feedback-service] ← Si no es ACCEPTED:
            9. Llama API LLM con prompt
           10. Persiste Feedback
           11. Notifica al cliente (WebSocket / polling)
```

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
