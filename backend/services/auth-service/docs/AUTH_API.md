# Auth Service — Documentación de API

> **Servicio:** `auth-service`  
> **Puerto:** `8081`  
> **Base URL:** `http://localhost:8081/api/v1/auth`  
> **Stack:** Java 17 · Spring Boot 3.2.4 · PostgreSQL · Redis · JWT (JJWT 0.12.5)

---

## Estado actual

| Funcionalidad | Estado |
|---|---|
| Registro de usuario (`POST /register`) | ✅ Implementado |
| Inicio de sesión (`POST /login`) | ✅ Implementado |
| Generación de Access Token (JWT) | ✅ Implementado |
| Generación de Refresh Token (JWT) | ✅ Implementado |
| Bloqueo por intentos fallidos (Redis) | ✅ Implementado |
| Filtro JWT para endpoints protegidos | 🔴 Pendiente |
| Refresh Token endpoint | 🔴 Pendiente |

---

## Requisitos para ejecutar localmente

### 1. Variables de entorno

```bash
DB_URL=jdbc:postgresql://localhost:5432/goslint_judge
DB_USERNAME=postgres
DB_PASSWORD=<tu_password>
```

Las siguientes tienen valores por defecto en `application.properties` y son opcionales:

```bash
REDIS_HOST=localhost       # default: localhost
REDIS_PORT=6379            # default: 6379
JWT_SECRET=<clave_hex>     # default: clave de desarrollo hardcodeada (NO usar en producción)
JWT_EXPIRATION_ACCESS=900000    # default: 15 minutos en ms
JWT_EXPIRATION_REFRESH=28800000 # default: 8 horas en ms
```

### 2. Servicios externos necesarios

- **PostgreSQL** corriendo en `localhost:5432` con la base de datos `goslint_judge`.
- **Redis** corriendo en `localhost:6379`.

```bash
# Instalar e iniciar Redis (macOS)
brew install redis
brew services start redis

# Verificar que Redis está activo
redis-cli ping  # Debe responder: PONG
```

### 3. Arrancar el servicio

Desde la raíz del proyecto (`backend/`):

```bash
DB_PASSWORD=<tu_password> \
DB_USERNAME=postgres \
DB_URL=jdbc:postgresql://localhost:5432/goslint_judge \
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
sh ./gradlew :services:auth-service:bootRun
```

---

## Endpoints

### `POST /api/v1/auth/register`

Registra un nuevo usuario en el sistema con rol `STUDENT` por defecto.

#### Request Body

```json
{
  "username": "jortorres",
  "email": "jorge@uceva.edu.co",
  "password": "password123",
  "institution": "UCEVA"
}
```

#### Validaciones

| Campo | Regla |
|---|---|
| `username` | Obligatorio · Entre 3 y 50 caracteres |
| `email` | Obligatorio · Formato válido (x@y.com) |
| `password` | Obligatorio · Mínimo 6 caracteres |
| `institution` | Opcional |

#### Respuesta exitosa — `201 Created`

```json
{
  "message": "Usuario registrado exitosamente",
  "userId": "a3f1bc2d-4e5f-6789-abcd-ef0123456789"
}
```

#### Respuestas de error

| HTTP Status | Cuándo ocurre | Ejemplo de body |
|---|---|---|
| `400 Bad Request` | Validación fallida en algún campo | `{ "email": "Debe proporcionar un correo electrónico válido" }` |
| `409 Conflict` | El email o username ya existe | `{ "error": "El correo electrónico ya está registrado." }` |

---

### `POST /api/v1/auth/login`

Autentica un usuario y devuelve un par de tokens JWT (access + refresh).

#### Request Body

```json
{
  "email": "jorge@uceva.edu.co",
  "password": "password123"
}
```

#### Validaciones

| Campo | Regla |
|---|---|
| `email` | Obligatorio · Formato válido |
| `password` | Obligatorio |

#### Respuesta exitosa — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresIn": 900000
}
```

| Campo | Descripción |
|---|---|
| `accessToken` | Token JWT de corta duración (15 min). Usar en cabecera `Authorization: Bearer <token>` |
| `refreshToken` | Token JWT de larga duración (8 horas). Guardado en Redis. Usar para renovar el access token |
| `type` | Tipo del token. Siempre `"Bearer"` |
| `expiresIn` | Tiempo de expiración del access token en **milisegundos** (900000 = 15 min) |

#### Respuestas de error

| HTTP Status | Cuándo ocurre | Body |
|---|---|---|
| `400 Bad Request` | Validación fallida | `{ "email": "..." }` |
| `401 Unauthorized` | Email o contraseña incorrectos (mensaje genérico, no revela si el usuario existe) | `{ "error": "Credenciales incorrectas" }` |
| `423 Locked` | La cuenta fue bloqueada por 5+ intentos fallidos | `{ "error": "Su cuenta ha sido bloqueada por 15 minutos..." }` |

> **Nota de seguridad:** El error `401` es genérico a propósito. No se revela si el usuario existe o no, para prevenir ataques de enumeración de usuarios.

---

## Estructura del JWT (Access Token)

El token se genera con los siguientes claims:

```json
{
  "sub": "a3f1bc2d-4e5f-6789-abcd-ef0123456789",   // UUID del usuario
  "iss": "goslint-auth-service",
  "email": "jorge@uceva.edu.co",
  "role": "STUDENT",
  "iat": 1726324800,   // Emitido en (timestamp)
  "exp": 1726325700    // Expira en (timestamp, 15 min después)
}
```

---

## Mecanismo de seguridad — Bloqueo por intentos fallidos

Implementado con Redis. Funciona así:

1. Cada intento de login fallido incrementa un contador en Redis con clave `auth:failed_attempts:<email>`.
2. El contador tiene TTL de 1 hora (se limpia solo después de una hora sin intentos).
3. Si el contador llega a **5 intentos fallidos**, la cuenta se bloquea en Redis con clave `auth:lock:<email>` por **15 minutos**.
4. Cualquier intento de login durante el bloqueo devuelve directamente `423 Locked` con el tiempo de espera restante.
5. Un login exitoso **limpia** el contador de intentos fallidos.

---

## Cómo probar con Postman o Thunder Client

### Paso 1: Registrar un usuario

```
POST http://localhost:8081/api/v1/auth/register
Content-Type: application/json

{
  "username": "jortorres",
  "email": "jorge@uceva.edu.co",
  "password": "password123",
  "institution": "UCEVA"
}
```

### Paso 2: Hacer login

```
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{
  "email": "jorge@uceva.edu.co",
  "password": "password123"
}
```

Copia el valor de `accessToken` de la respuesta.

### Paso 3: Usar el token en endpoints protegidos (cuando estén disponibles)

```
GET http://localhost:8082/api/v1/problems
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Arquitectura interna (Clean Architecture)

```
auth-service/
├── domain/
│   ├── model/
│   │   ├── User.java              ← Entidad de dominio pura
│   │   ├── AuthToken.java         ← Value object para los tokens
│   │   └── Role.java              ← Enum: STUDENT, ADMIN, ORGANIZER, SERVICE
│   ├── exception/
│   │   ├── UserAlreadyExistsException.java
│   │   ├── BadCredentialsException.java
│   │   ├── AccountLockedException.java
│   │   └── InvalidUserDataException.java
│   ├── repository/
│   │   ├── UserRepository.java        ← Interfaz (contrato para persistencia)
│   │   └── AuthCacheRepository.java   ← Interfaz (contrato para Redis)
│   └── service/
│       ├── TokenProvider.java     ← Interfaz (contrato para generación de JWT)
│       └── PasswordEncoder.java   ← Interfaz (contrato para BCrypt)
│
├── application/
│   └── usecase/
│       ├── RegisterUserUseCase.java   ← Interfaz del caso de uso
│       ├── LoginUserUseCase.java      ← Interfaz del caso de uso
│       └── impl/
│           ├── RegisterUserUseCaseImpl.java  ← Lógica de registro
│           └── LoginUserUseCaseImpl.java     ← Lógica de login + control de intentos
│
└── infrastructure/
    ├── web/
    │   ├── controller/
    │   │   ├── AuthController.java        ← Endpoints REST
    │   │   └── AuthExceptionHandler.java  ← Manejo global de errores
    │   └── dto/
    │       ├── RegisterRequest.java
    │       ├── LoginRequest.java
    │       └── LoginResponse.java
    ├── persistence/
    │   ├── entity/    UserJpaEntity.java
    │   ├── repository/ SpringDataUserRepository.java
    │   └── adapter/   UserRepositoryAdapter.java
    ├── cache/
    │   └── RedisCacheAdapter.java     ← Implementa AuthCacheRepository con Redis
    ├── security/
    │   └── JwtTokenProviderAdapter.java  ← Implementa TokenProvider con JJWT
    └── config/
        └── SecurityConfig.java        ← Spring Security + Bean PasswordEncoder
```
