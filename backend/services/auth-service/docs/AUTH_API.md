# Auth Service — Documentación de API

> **Servicio:** `auth-service`  
> **Puerto:** `8081`  
> **Base URL:** `http://localhost:8081/api/v1/auth`  
> **Stack:** Java 17 · Spring Boot 3.2.4 · PostgreSQL · Redis · JWT (JJWT 0.12.5)

---

## Estado actual

| Funcionalidad | Estado |
|---|---|
| Registro (`POST /register`) | ✅ |
| Inicio de sesión (`POST /login`) | ✅ |
| Cierre de sesión (`POST /logout`) | ✅ |
| Renovación de tokens (`POST /refresh`) | ✅ |
| Perfil (`GET /me`), cambio de usuario (`PATCH /me/username`) y de contraseña (`PATCH /me/password`) | ✅ |
| Bloqueo por intentos fallidos (Redis) | ✅ |
| Lista negra de access tokens (Redis) | ✅ |
| Disponibilidad de username (`GET /check-username`) | ✅ |
| Administración de usuarios (`/api/v1/admin/users`) | ✅ |
| Super administrador inicial por variables de entorno | ✅ |

---

## Requisitos para ejecutar localmente

### 1. Variables de entorno

```bash
DB_URL=jdbc:postgresql://localhost:5432/goslint_judge_db
DB_USERNAME=postgres
DB_PASSWORD=<tu_password>
```

Opcionales (valores por defecto en `application.properties`):

```bash
REDIS_HOST=localhost            # default: localhost
REDIS_PORT=6379                 # default: 6379
JWT_SECRET=<clave>              # mínimo 32 caracteres; el default es solo para desarrollo (NO usar en producción)
JWT_EXPIRATION_ACCESS=900000    # 15 minutos en ms
JWT_EXPIRATION_REFRESH=28800000 # 8 horas en ms
JPA_SHOW_SQL=false              # true para ver el SQL en consola

SUPER_ADMIN_EMAIL=<correo>      # crea el super administrador al arrancar (ver "Roles")
SUPER_ADMIN_PASSWORD=<clave>    # misma política de contraseñas; sin ella no se crea
SUPER_ADMIN_USERNAME=superadmin # default: superadmin
```

### 2. Servicios externos

- **PostgreSQL** en `localhost:5432` con la base `goslint_judge_db`.
- **Redis** en `localhost:6379`.

### 3. Arrancar

Desde `backend/`:

```bash
./gradlew :services:auth-service:bootRun
```

---

## Endpoints

Todos los errores tienen la forma `{ "error": "mensaje" }`, salvo `400` por validación, que devuelve `{ "campo": "mensaje" }`.

### Política de campos

| Campo | Regla |
|---|---|
| `username` | 3–30 caracteres: letras, dígitos, `_` y `.` · único sin distinguir mayúsculas |
| `email` | Formato válido · se guarda en minúsculas · único |
| `password` | Mínimo 8 caracteres, con al menos una mayúscula, un número y un carácter especial (en registro **y** en cambio de contraseña) |
| `firstName`, `lastName` | 2–50 caracteres |
| `institution` | Opcional · máximo 255 caracteres |

### `POST /register`

```json
{ "firstName": "Jorge", "lastName": "Torres", "username": "jortorres",
  "email": "jorge@uceva.edu.co", "password": "Secreta1!", "institution": "UCEVA" }
```

| Status | Cuándo |
|---|---|
| `201` | `{ "message": "...", "userId": "<uuid>" }` |
| `400` | Validación fallida |
| `409` | Correo o username ya registrados |

### `POST /login`

```json
{ "email": "jorge@uceva.edu.co", "password": "Secreta1!" }
```

`200`:

```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "type": "Bearer", "expiresIn": 900000 }
```

`expiresIn` es la vigencia del access token en milisegundos.

| Status | Cuándo |
|---|---|
| `400` | Validación fallida |
| `401` | Credenciales incorrectas. El mensaje es el mismo si el correo no existe, la contraseña falla o la cuenta está inactiva (evita enumerar usuarios) |
| `423` | Cuenta bloqueada por 5 intentos fallidos |

### `POST /logout`

Header `Authorization: Bearer <accessToken>`. Siempre responde `200`, aunque el token falte o sea inválido. Si es válido, lo revoca hasta que expire y borra el refresh token del usuario.

### `POST /refresh`

```json
{ "refreshToken": "eyJ..." }
```

`200` con un par nuevo (misma forma que `/login`). El refresh token usado deja de servir (rotación).

| Status | Cuándo |
|---|---|
| `400` | Falta `refreshToken` |
| `401` | No es un refresh token, expiró, no es el vigente (ya rotado o sesión cerrada) o el usuario no existe |
| `403` | Cuenta desactivada |

### `GET /check-username?username=<valor>`

Público. Sirve para validar el formulario mientras se escribe. `200 { "available": true|false }`; `400` si el formato es inválido. Si se envía un access token válido, el username propio cuenta como disponible. No distingue mayúsculas.

### `GET /me`

Header `Authorization: Bearer <accessToken>`. `200`:

```json
{ "id": "<uuid>", "firstName": "Jorge", "lastName": "Torres", "username": "jortorres",
  "email": "jorge@uceva.edu.co", "role": "STUDENT", "institution": "UCEVA",
  "isActive": true, "createdAt": "2026-03-01T10:30:00" }
```

`401` si el token falta, es inválido, expiró, es un refresh token o fue revocado · `403` si la cuenta está desactivada.

### `PATCH /me/username`

```json
{ "username": "nuevo_user" }
```

`200` con el perfil actualizado · `400` formato inválido · `401` token inválido · `409` username en uso. El dueño puede cambiar solo la capitalización del suyo.

### `PATCH /me/password`

```json
{ "currentPassword": "Secreta1!", "newPassword": "Otra2@clave" }
```

`204` sin cuerpo · `400` la nueva no cumple la política o es igual a la actual · `401` token inválido · `422` la contraseña actual es incorrecta.

**Cierra todas las sesiones:** revoca el access token usado y borra el refresh token, así que el cliente debe volver a iniciar sesión.

El correo no se puede modificar desde el perfil.

---

## Roles y administración de usuarios

| Rol | Cómo se obtiene | Puede |
|---|---|---|
| `STUDENT` | Registro (siempre; el registro ignora cualquier `role` que envíe el cliente) | Resolver problemas y enviar soluciones |
| `ORGANIZER` | Lo asigna un `ADMIN` o `SUPER_ADMIN` | Gestionar problemas y concursos |
| `ADMIN` | Lo asigna solo el `SUPER_ADMIN` | Lo anterior + administrar estudiantes y organizadores |
| `SUPER_ADMIN` | Solo al arrancar el servicio, con `SUPER_ADMIN_EMAIL` y `SUPER_ADMIN_PASSWORD` | Todo, incluida la gestión de administradores |

El super administrador se crea una sola vez: si ya existe uno, las variables se ignoran. Si el correo ya pertenece a una cuenta, esa cuenta se promueve. No se puede asignar, modificar ni desactivar por la API. Los demás servicios tratan a `SUPER_ADMIN` como `ADMIN`.

Todos los endpoints de `/api/v1/admin/users` exigen `Authorization: Bearer <accessToken>` y leen el rol del usuario **en la base de datos** (no del JWT). Sin token válido responden `401`; si el dueño no es administrador, `403`.

**Reglas de jerarquía** (todas responden `403` al incumplirse):

- Nadie cambia su propio rol ni desactiva su propia cuenta.
- Nadie asigna `SUPER_ADMIN` ni modifica la cuenta de un `SUPER_ADMIN`.
- Un `ADMIN` solo gestiona cuentas `STUDENT` y `ORGANIZER`, y solo puede asignar esos dos roles. Los administradores los gestiona el `SUPER_ADMIN`.

### `GET /api/v1/admin/users?page=0&size=20&search=`

`search` (opcional) coincide parcialmente con username o correo, sin distinguir mayúsculas. `size` máximo 100. Orden: más recientes primero.

```json
{ "content": [ { "id": "<uuid>", "firstName": "Camila", "lastName": "Restrepo", "username": "camila_dev",
                 "email": "camila@uceva.edu.co", "role": "STUDENT", "institution": "UCEVA",
                 "isActive": true, "createdAt": "2026-03-01T10:30:00" } ],
  "page": 0, "size": 20, "totalPages": 3, "totalElements": 41 }
```

### `PATCH /api/v1/admin/users/{id}/role`

```json
{ "role": "ORGANIZER" }
```

`200` con el usuario actualizado · `400` rol inexistente o ausente · `403` regla de jerarquía · `404` usuario inexistente. Cierra la sesión del usuario: el rol nuevo rige cuando venza su access token (máx. 15 min).

### `PATCH /api/v1/admin/users/{id}/status`

```json
{ "isActive": false }
```

`200` con el usuario actualizado · `400` falta `isActive` · `403` regla de jerarquía (incluye desactivarse a sí mismo) · `404`. Al desactivar se cierra su sesión: no puede renovar ni consultar su perfil, pero un access token ya emitido sigue valiendo hasta 15 min en los demás servicios.

---

## Tokens JWT

Access y refresh token comparten claims y se distinguen por `type`:

```json
{
  "jti": "uuid-único",
  "sub": "a3f1bc2d-4e5f-6789-abcd-ef0123456789",
  "iss": "goslint-judge",
  "email": "jorge@uceva.edu.co",
  "role": "STUDENT",
  "type": "access",
  "iat": 1726324800,
  "exp": 1726325700
}
```

| Tipo | Vigencia | Uso |
|---|---|---|
| `access` | 15 min | Header `Authorization: Bearer` en todos los servicios |
| `refresh` | 8 h | Solo en `POST /refresh`. `JwtTokenValidator` (módulo compartido) lo rechaza como credencial de acceso |

---

## Redis

| Clave | TTL | Contenido |
|---|---|---|
| `auth:failed_attempts:<email>` | 1 h desde el primer fallo | Contador de intentos fallidos |
| `auth:lock:<email>` | 15 min | Bloqueo de la cuenta |
| `auth:refresh_token:<userId>` | 8 h | Único refresh token vigente del usuario |
| `auth:blacklist:<sha256(token)>` | Vigencia restante del token | Access tokens revocados |

### Bloqueo por intentos fallidos

1. Cada login fallido suma al contador del correo (en minúsculas).
2. Al quinto fallo el correo se bloquea 15 minutos y el contador se reinicia.
3. Mientras dura el bloqueo, el login responde `423` con los minutos restantes (redondeados hacia arriba).
4. Un login exitoso borra el contador.

---

## Estructura del código

Capas planas de Spring Boot, sin interfaces de casos de uso ni adaptadores:

```
co.uceva.auth/
├── controller/   AuthController, AuthExceptionHandler
├── service/      AuthService (register, login, logout, refresh)
│                 ProfileService (me, username, password, check-username)
│                 AdminUserService (listado, rol y estado de usuarios)
├── repository/   UserRepository (Spring Data JPA)
├── model/        User (@Entity), Role
├── dto/          Requests/responses (records) y ValidationRules
├── exception/    Excepciones de negocio, mapeadas a HTTP por AuthExceptionHandler
├── security/     JwtService (emite y lee JWT)
├── cache/        AuthCache (Redis; implementa TokenBlacklist del módulo compartido)
└── config/       SecurityConfig (CORS, cadena de filtros, BCrypt)
                  SuperAdminInitializer (crea el super administrador al arrancar)
```
