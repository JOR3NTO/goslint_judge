# Docker de Goslint Judge — guía paso a paso

Levanta todo el backend con un solo comando: PostgreSQL, Redis, RabbitMQ y los servicios
`auth-service`, `problem-service` y `submission-service`. El `judge-service` (el sandbox)
va aparte, en un perfil que **solo funciona en Linux** (ver [sección 6](#6-judge-service-solo-linux)).

## 1. Qué hay en el compose

```
                 ┌────────────────────────── red interna de compose ──────────────────────────┐
 navegador ──►   │ auth-service :8081 ──► postgres :5432                                       │
 (puertos        │        └───────────────► redis :6379                                        │
  publicados)    │ problem-service :8082 ─► postgres                                           │
                 │ submission-service :8083 ► postgres, rabbitmq :5672                         │
                 │ judge-service :8084 (perfil sandbox) ► rabbitmq, redis, problem-service     │
                 └─────────────────────────────────────────────────────────────────────────────┘
```

| Servicio | Imagen | Para qué | Puerto |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | Una sola base (`goslint_judge_db`) para todos | 5432 |
| `redis` | `redis:7-alpine` | Sesiones, bloqueo de login, lista negra de tokens | 6379 |
| `rabbitmq` | `rabbitmq:3.13-management` | Cola de envíos hacia el juez | 5672 / 15672 (consola) |
| `auth-service` | se construye aquí | Registro, login, perfil, administración de usuarios | 8081 |
| `problem-service` | se construye aquí | Problemas y casos de prueba | 8082 |
| `submission-service` | se construye aquí | Envíos y veredictos (WebSocket) | 8083 |
| `judge-service` | se construye aquí (perfil `sandbox`) | Evalúa el código en un sandbox | 8084 |

Archivos que intervienen:

| Archivo | Qué hace |
|---|---|
| `infrastructure/docker/docker-compose.yml` | Define los servicios, sus variables, volúmenes y el orden de arranque |
| `infrastructure/docker/.env` | Tus valores locales y secretos. **No se sube a git** |
| `backend/docker/spring-service.Dockerfile` | Receta para construir la imagen de cualquier servicio Spring |
| `backend/.dockerignore` | Qué NO se envía a Docker al construir (carpetas `build`, `.env`...) |

## 2. Requisitos

- **Docker Desktop** (Windows/Mac) o **Docker Engine + Compose v2** (Linux). En Windows debe estar
  *encendido* (el icono de la ballena, "Engine running") y usar el backend **WSL 2**.
- Comprobar: `docker version` y `docker compose version` no deben dar error.
- Puertos libres: 5432, 6379, 5672, 15672, 8081-8083. Si ya tienes un PostgreSQL local en el 5432,
  cambia `POSTGRES_PORT` en el `.env`.
- Unos 4 GB de RAM para Docker (Docker Desktop → Settings → Resources).

## 3. Primer arranque

Desde `infrastructure/docker/`:

```bash
# 1. Crear tu archivo de variables
cp .env.example .env

# 2. Generar un secreto para los JWT y pegarlo en JWT_SECRET (en el .env)
openssl rand -hex 32          # en Windows sin openssl:  python -c "import secrets; print(secrets.token_hex(32))"

# 3. (Opcional) datos del super administrador, en el .env
#    SUPER_ADMIN_EMAIL=admin@uceva.local
#    SUPER_ADMIN_PASSWORD=Admin1234!       # mayuscula + numero + caracter especial, minimo 8

# 4. Construir las imágenes y levantar todo
docker compose up -d --build

# 5. Ver el estado: los servicios deben quedar "healthy"
docker compose ps
```

La **primera** construcción tarda varios minutos (descarga Gradle y las dependencias). Las siguientes
reutilizan la caché y tardan segundos.

Comprobar que responde:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/api/v1/auth/check-username?username=prueba   # 200
```

Consola de RabbitMQ: <http://localhost:15672> (usuario/clave del `.env`, por defecto `guest`/`guest`).

## 4. Comandos del día a día

| Qué quieres | Comando |
|---|---|
| Ver estado y salud | `docker compose ps` |
| Ver logs de un servicio (seguir) | `docker compose logs -f auth-service` |
| Reiniciar un servicio | `docker compose restart auth-service` |
| Reconstruir tras cambiar código | `docker compose up -d --build auth-service` |
| Entrar al contenedor | `docker compose exec auth-service bash` |
| Entrar a PostgreSQL | `docker compose exec postgres psql -U postgres -d goslint_judge_db` |
| Ver la cola de RabbitMQ | consola web, pestaña *Queues* |
| Apagar (conserva los datos) | `docker compose down` |
| Apagar **y borrar los datos** | `docker compose down -v` |
| Solo la infraestructura (para correr los servicios con Gradle en tu IDE) | `docker compose up -d postgres redis rabbitmq` |

> `down -v` borra los volúmenes: pierdes la base de datos. Úsalo para empezar de cero.

## 5. Cómo funciona (para entender, no solo copiar)

**Un contenedor es un localhost distinto.** Dentro de `auth-service`, `localhost` es el propio
contenedor. Por eso `DB_URL` apunta a `postgres:5432` y no a `localhost:5432`: en la red de compose
cada servicio se encuentra por su **nombre**. Los `ports:` solo sirven para entrar desde TU máquina.

**El Dockerfile tiene dos etapas** (`backend/docker/spring-service.Dockerfile`):
1. *build*: JDK + Gradle compilan el `.jar` (pesa ~1 GB, se descarta).
2. *ejecución*: solo un JRE y el `.jar`, con un usuario sin privilegios.
Así la imagen final es pequeña y no lleva compiladores ni código fuente.

**Un Dockerfile para tres servicios.** El argumento `SERVICE` (`args:` en el compose) elige qué módulo
de Gradle compilar. Agregar un servicio nuevo = copiar un bloque del compose y cambiar `SERVICE`.

**El orden de arranque** lo controla `depends_on` con `condition: service_healthy`: `auth-service` no
arranca hasta que PostgreSQL y Redis responden a su *healthcheck*. Sin eso, el servicio intenta conectar
antes de que la base esté lista y muere.

**Anclas YAML** (`x-jwt-env`, `x-db-env` + `<<: *jwt-env`): se define el bloque una vez y se reutiliza.
Así el `JWT_SECRET` es idéntico en todos los servicios, que es obligatorio: `auth-service` firma los
tokens y los demás los validan; con secretos distintos todo da 401.

**Variables.** Compose lee `infrastructure/docker/.env` y reemplaza `${VAR:-valor}` (con valor por
defecto) o `${VAR:?mensaje}` (obligatoria: si falta, compose se detiene con ese mensaje).
El `.env` tiene secretos: está en `.gitignore`. Lo que se sube es `.env.example`.

**Volúmenes.** `postgres-data`, `redis-data` y `rabbitmq-data` guardan los datos fuera del
contenedor: puedes destruir y recrear contenedores sin perder usuarios ni colas.

**Esquema de la base de datos.** `problem-service` y `submission-service` usan Flyway (migraciones
versionadas, cada uno con su propia tabla de historial). `auth-service` aún usa `ddl-auto=update`
(Hibernate crea la tabla `users`). Todos comparten la misma base.

## 6. judge-service (solo Linux)

El sandbox usa *cgroups v2*, `bubblewrap` y una *slice* de systemd en el host. **No funciona en
Docker Desktop (Windows/Mac)**: necesita Docker Engine nativo en Linux, kernel ≥ 5.19. Requisitos
completos en `backend/services/judge-service/docs/REQUIREMENTS.md`.

En un host Linux preparado:

```bash
sudo cp ../systemd/goslint.slice /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now goslint.slice

docker compose --profile sandbox up -d --build        # levanta TODO, judge incluido
docker logs goslint-judge | grep "delegacion de cgroups OK"
docker exec -u ubuntu goslint-judge sh /opt/judge/prueba_humo.sh
```

En Windows, el resto del sistema funciona completo; los envíos de código quedan encolados en
RabbitMQ sin que nadie los evalúe hasta que corra un judge (en el Linux de tu compañero).

## 7. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `error during connect ... dockerDesktopLinuxEngine` | Docker Desktop no está encendido. Ábrelo y espera a "Engine running" |
| `required variable JWT_SECRET is missing` | Falta `.env` o `JWT_SECRET` vacío. Ver paso 3 |
| `port is already allocated` / `address already in use` | Otro programa usa ese puerto. Cámbialo en el `.env` (p. ej. `POSTGRES_PORT=5433`) |
| `/bin/sh^M: bad interpreter` al construir | Archivos con finales de línea de Windows (CRLF). El repo trae `.gitattributes`; haz `git add --renormalize .` y vuelve a clonar/checkout |
| Un servicio queda `unhealthy` o reinicia en bucle | `docker compose logs <servicio>`: casi siempre es la base de datos o el `JWT_SECRET` |
| `password authentication failed` en PostgreSQL | Cambiaste `POSTGRES_PASSWORD` después de crear el volumen. Haz `docker compose down -v` |
| Todo da 401 entre servicios | `JWT_SECRET` o `JWT_ISSUER` distintos entre servicios, o tokens de antes de cambiarlos |
| Cambié código y no se nota | Falta `--build`: `docker compose up -d --build <servicio>` |
| El build es lento | Normal la primera vez. Dale más CPU/RAM a Docker Desktop |
