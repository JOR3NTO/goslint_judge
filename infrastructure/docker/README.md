# Docker de Goslint Judge — guía paso a paso

> ⚠️ **El compose está parcialmente desactualizado respecto al código.** El backend pasó a ser un
> monolito modular: `auth`, `problem`, `contest` y `submission` viven en un único servicio,
> `contest-platform` (puerto 8080), que **no está declarado en `docker-compose.yml`**. Los bloques
> `auth-service`, `problem-service`, `submission-service` y `contest-service` del compose apuntan a
> módulos Gradle que ya no existen (el build fallaría) y a los puertos 8081–8086.
>
> Hoy el compose sirve para tres cosas: **la infraestructura** (`postgres`, `redis`, `rabbitmq`),
> **el juez** (`judge-service`, perfil `sandbox`, solo Linux) y, en un host Linux, ambas a la vez.
> `contest-platform` se corre en el host con Gradle: `./gradlew :services:contest-platform:bootRun`.

Levanta con un solo comando PostgreSQL, Redis y RabbitMQ. El `judge-service` (el sandbox) va aparte,
en un perfil que **solo funciona en Linux** (ver [sección 6](#6-judge-service-solo-linux)).

## 1. Qué hay en el compose

```
 host (tu máquina)                       red interna de compose
 ┌───────────────────────┐            ┌──────────────────────────────────────────┐
 │ contest-platform :8080│ ─────────► │ postgres :5432   redis :6379             │
 │ (Gradle, fuera del    │            │ rabbitmq :5672 (consola :15672)          │
 │  compose)             │ ◄───────── │ judge-service :8084 (perfil sandbox)     │
 └───────────────────────┘  HTTP/AMQP └──────────────────────────────────────────┘
```

| Servicio | Imagen | Para qué | Puerto | Estado |
|---|---|---|---|---|
| `postgres` | `postgres:16-alpine` | Una sola base (`goslint_judge_db`) para todos los módulos | 5432 | Vigente |
| `redis` | `redis:7-alpine` | Módulo `auth`: bloqueo de login, refresh tokens, lista negra de tokens | 6379 | Vigente |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | Cola de envíos hacia el juez y veredictos de vuelta | 5672 / 15672 (consola) | Vigente |
| `judge-service` | se construye aquí (perfil `sandbox`) | Evalúa el código en un sandbox | 8084 | Vigente, con dos puntos desactualizados (ver [sección 6](#6-judge-service-solo-linux)) |
| `auth-service`, `problem-service`, `submission-service`, `contest-service` | `spring-service.Dockerfile` | Servicios anteriores al monolito | 8081, 8082, 8083, 8086 | **Obsoletos**: sus módulos Gradle ya no existen |

Volúmenes: `postgres-data`, `redis-data` y `rabbitmq-data`.

Archivos que intervienen:

| Archivo | Qué hace |
|---|---|
| `infrastructure/docker/docker-compose.yml` | Define los servicios, sus variables, volúmenes y el orden de arranque |
| `infrastructure/docker/.env` | Tus valores locales y secretos. **No se sube a git** (la plantilla es `.env.example`) |
| `infrastructure/systemd/goslint.slice` | Rama de cgroups v2 del host donde nace el contenedor del juez (solo Linux) |
| `backend/services/judge-service/docker/Dockerfile` | Imagen del juez (con `bubblewrap`, seccomp y compiladores) |
| `backend/docker/spring-service.Dockerfile` | Imagen genérica de un servicio Spring (argumento `SERVICE`); hoy solo la usan los bloques obsoletos del compose |
| `backend/.dockerignore` | Qué NO se envía a Docker al construir (carpetas `build`, `.env`...) |

## 2. Requisitos

- **Docker Desktop** (Windows/Mac) o **Docker Engine + Compose v2** (Linux). En Windows debe estar
  *encendido* (el icono de la ballena, "Engine running") y usar el backend **WSL 2**.
- Comprobar: `docker version` y `docker compose version` no deben dar error.
- Puertos libres: 5432, 6379, 5672, 15672 (y 8084 si corres el juez). Si ya tienes un PostgreSQL
  local en el 5432, cambia `POSTGRES_PORT` en el `.env`.
- JDK 17 para correr `contest-platform` con Gradle en el host.

## 3. Primer arranque (infraestructura + contest-platform)

Desde `infrastructure/docker/`:

```bash
# 1. Crear tu archivo de variables
cp .env.example .env

# 2. Generar un secreto para los JWT y pegarlo en JWT_SECRET (en el .env)
openssl rand -hex 32          # en Windows sin openssl:  python -c "import secrets; print(secrets.token_hex(32))"

# 3. Levantar solo la infraestructura
docker compose up -d postgres redis rabbitmq

# 4. Ver el estado: los tres deben quedar "healthy"
docker compose ps
```

Luego, desde `backend/`, correr `contest-platform` en el host con las variables de `backend/.env`
(plantilla en `backend/.env.example`; `DB_PASSWORD` debe coincidir con `POSTGRES_PASSWORD`, y
`JWT_SECRET` debe ser el mismo secreto):

```bash
set -a && source .env && set +a
./gradlew :services:contest-platform:bootRun
```

`contest-platform` aplica las migraciones Flyway al arrancar. Para crear el super administrador,
define `SUPER_ADMIN_EMAIL` y `SUPER_ADMIN_PASSWORD` (mayúscula, número y carácter especial, mínimo 8).

Comprobar que responde:

```bash
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8080/api/v1/auth/check-username?username=prueba"   # 200
```

Consola de RabbitMQ: <http://localhost:15672> (usuario/clave del `.env`, por defecto `guest`/`guest`).

Variables del `.env.example` de esta carpeta:

| Variable | Uso |
|---|---|
| `JWT_SECRET` | **Obligatoria**, mínimo 32 caracteres. El compose la declara con `${JWT_SECRET:?...}` |
| `JWT_ISSUER` | Emisor de los tokens (`goslint-judge`) |
| `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`, `SUPER_ADMIN_USERNAME` | Super administrador inicial (los consume `contest-platform`, no el compose actual del juez) |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT` | PostgreSQL |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_PORT`, `RABBITMQ_MANAGEMENT_PORT` | RabbitMQ |
| `REDIS_PORT` | Puerto publicado de Redis |
| `AUTH_PORT`, `PROBLEM_PORT`, `SUBMISSION_PORT`, `CONTEST_PORT` | Puertos de los servicios obsoletos |
| `CORS_ALLOWED_ORIGINS` | Orígenes del navegador (la usaban los servicios obsoletos; `contest-platform` la lee de su propio entorno) |
| `JUDGE_PORT`, `JUDGE_WORKERS`, `JUDGE_SANDBOX_LOG_LEVEL` | Opcionales, para el juez (comentadas en el ejemplo) |

## 4. Comandos del día a día

| Qué quieres | Comando |
|---|---|
| Ver estado y salud | `docker compose ps` |
| Ver logs de un servicio (seguir) | `docker compose logs -f rabbitmq` |
| Reiniciar un servicio | `docker compose restart postgres` |
| Entrar a PostgreSQL | `docker compose exec postgres psql -U postgres -d goslint_judge_db` |
| Ver la cola de RabbitMQ | consola web, pestaña *Queues* (`submission.evaluate`, `submission.judging`, `submission.judged` y sus `.dlq`) |
| Apagar (conserva los datos) | `docker compose down` |
| Apagar **y borrar los datos** | `docker compose down -v` |

> `down -v` borra los volúmenes: pierdes la base de datos. Úsalo para empezar de cero.

## 5. Cómo funciona (para entender, no solo copiar)

**Un contenedor es un localhost distinto.** Dentro de un contenedor, `localhost` es el propio
contenedor: a PostgreSQL se llega por el nombre del servicio (`postgres:5432`), no por
`localhost:5432`. Los `ports:` solo sirven para entrar desde TU máquina. Por eso `contest-platform`,
que corre en el host, usa `localhost` (`DB_URL`, `RABBITMQ_HOST`, `REDIS_HOST`), mientras que el
juez, que corre en un contenedor, necesita `host.docker.internal` para llamar a `contest-platform`.

**El orden de arranque** lo controla `depends_on` con `condition: service_healthy`, apoyado en los
*healthchecks* de `postgres`, `redis` y `rabbitmq`.

**Anclas YAML** (`x-jwt-env`, `x-db-env` + `<<: *jwt-env`): se define el bloque una vez y se reutiliza.
El `JWT_SECRET` debe ser idéntico en `contest-platform` y en el juez: `contest-platform` firma los
tokens y el juez firma los suyos (`role=SERVICE`); cada uno valida los del otro. Con secretos
distintos todo da 401.

**Variables.** Compose lee `infrastructure/docker/.env` y reemplaza `${VAR:-valor}` (con valor por
defecto) o `${VAR:?mensaje}` (obligatoria: si falta, compose se detiene con ese mensaje).
El `.env` tiene secretos: está en `.gitignore`. Lo que se sube es `.env.example`.

**Volúmenes.** `postgres-data`, `redis-data` y `rabbitmq-data` guardan los datos fuera del
contenedor: puedes destruir y recrear contenedores sin perder usuarios ni colas.

**Esquema de la base de datos.** Lo gestiona Flyway desde `contest-platform` (migraciones `V1`–`V11`,
historial en `flyway_schema_history_contest`). `JPA_DDL_AUTO` vale `update` por defecto.

## 6. judge-service (solo Linux)

El sandbox usa *cgroups v2*, `bubblewrap` y una *slice* de systemd en el host. **No funciona en
Docker Desktop (Windows/Mac)**: necesita Docker Engine nativo en Linux, kernel ≥ 5.19. Requisitos
completos en `backend/services/judge-service/docs/REQUIREMENTS.md`.

Lo que el compose declara para el juez (perfil `sandbox`, no arranca con un `docker compose up` normal):

- Imagen `goslint-judge:dev`, contenedor `goslint-judge`, puerto `8084` (`JUDGE_PORT`).
- `cgroup: host` y `cgroup_parent: goslint.slice`; bind de `/sys/fs/cgroup/goslint.slice` a `/cg`.
- `tmpfs` en `/work` (con `exec`, tamaño `JUDGE_WORK_SIZE`, 256 MB por defecto).
- `security_opt`: `seccomp=unconfined`, `apparmor=unconfined`, `systempaths=unconfined` (el código evaluado
  recibe el filtro seccomp propio de `bwrap`). Sin `privileged` ni `cap_add`.
- Variables: `RABBITMQ_*`, `SPRING_DATA_REDIS_HOST=redis`, `PROBLEM_SERVICE_URL`, `JWT_SECRET`, `JWT_ISSUER`,
  `JUDGE_SANDBOX_LOG_LEVEL`, `JUDGE_WORKERS` (2) y `JUDGE_EVALUATION_TIMEOUT_SECONDS` (300).

**Dos puntos desactualizados del bloque del juez:** `PROBLEM_SERVICE_URL` vale por defecto
`http://problem-service:8082` y `depends_on` exige `problem-service`, que ya no existe. Para usarlo con
`contest-platform` corriendo en el host, define `PROBLEM_SERVICE_URL=http://host.docker.internal:8080`
en el `.env` y levanta el juez sin sus dependencias declaradas (la infraestructura ya está arriba).

En un host Linux preparado:

```bash
sudo cp ../systemd/goslint.slice /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now goslint.slice

# infraestructura + juez (sin arrastrar el problem-service obsoleto)
docker compose up -d postgres redis rabbitmq
docker compose --profile sandbox up -d --build --no-deps judge-service

docker logs goslint-judge | grep "delegacion de cgroups OK"
docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh
```

En Windows, el resto del sistema funciona completo (infraestructura, `contest-platform` y los frontends);
los envíos de código quedan encolados en RabbitMQ sin que nadie los evalúe hasta que corra un juez en un
host Linux.

## 7. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `error during connect ... dockerDesktopLinuxEngine` | Docker Desktop no está encendido. Ábrelo y espera a "Engine running" |
| `required variable JWT_SECRET is missing` | Falta `.env` o `JWT_SECRET` vacío. Ver paso 2 de la sección 3 |
| `port is already allocated` / `address already in use` | Otro programa usa ese puerto. Cámbialo en el `.env` (p. ej. `POSTGRES_PORT=5433`) |
| `/bin/sh^M: bad interpreter` al construir | Archivos con finales de línea de Windows (CRLF). El repo trae `.gitattributes`; haz `git add --renormalize .` y vuelve a clonar/checkout |
| Un servicio queda `unhealthy` o reinicia en bucle | `docker compose logs <servicio>`: casi siempre es la base de datos o el `JWT_SECRET` |
| `password authentication failed` en PostgreSQL | Cambiaste `POSTGRES_PASSWORD` después de crear el volumen. Haz `docker compose down -v` |
| Todo da 401 entre `contest-platform` y el juez | `JWT_SECRET` o `JWT_ISSUER` distintos entre ambos, o tokens de antes de cambiarlos |
| `contest-platform` no arranca | Falta `JWT_SECRET` (obligatoria) o `DB_PASSWORD`, o PostgreSQL/Redis no están arriba |
| Un envío queda en `PENDING` | `contest-platform` no pudo publicar en RabbitMQ; se reintenta cada 30 s cuando el broker vuelva |
| Un envío queda en `QUEUED` | No hay un `judge-service` consumiendo `submission.evaluate` (p. ej. en Windows) |
| El build del compose falla con `auth-service`, `problem-service`... | Son bloques obsoletos: levanta solo `postgres redis rabbitmq` (y el juez con `--no-deps`) |
