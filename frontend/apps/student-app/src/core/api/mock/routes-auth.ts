/**
 * Rutas de auth-service (`/v1/auth/**`) en el mock.
 *
 * Reproduce las validaciones de RegisterRequest / LoginRequest y los códigos de
 * AuthExceptionHandler: 400 con `{ campo: msg }`, 401 y 409 con `{ error }`, 423 al bloquear.
 */
import {
  LOGIN_LOCK_MS,
  LOGIN_MAX_FAILED_ATTEMPTS,
  type MockState,
  type UserRecord,
} from "./model"
import { digest, newId, isoAt } from "./util"
import {
  ApiRouter,
  created,
  fieldErrors,
  httpError,
  ok,
  readBody,
  textField,
  type MockContext,
  type MockRequest,
} from "./router"
import { authenticate, issueSession, revokeSession } from "./session"
import { toUserProfile } from "./domain"

// Reglas copiadas de backend/services/auth-service ValidationRules.java.
const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/
const PASSWORD_MESSAGE =
  "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial"
const USERNAME_PATTERN = /^[a-zA-Z0-9_.]{3,30}$/
const USERNAME_MESSAGE =
  "El nombre de usuario debe tener entre 3 y 30 caracteres y solo puede contener letras, dígitos, guion bajo (_) y punto (.)"
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const LOCKED_MESSAGE = "Su cuenta ha sido bloqueada por 15 minutos debido a múltiples intentos fallidos."

/** Texto recortado del body; cadena vacía si el campo falta o no es texto. */
function text(body: Record<string, unknown>, name: string): string {
  return (textField(body, name) ?? "").trim()
}

function findUserByEmail(state: MockState, email: string): UserRecord | undefined {
  const wanted = email.toLowerCase()
  return state.users.find((u) => u.email.toLowerCase() === wanted)
}

function register(req: MockRequest, ctx: MockContext) {
  const body = readBody(req, "error")
  const errors: Record<string, string> = {}

  const firstName = text(body, "firstName")
  if (!firstName) errors.firstName = "El nombre es obligatorio"
  else if (firstName.length < 2 || firstName.length > 50) errors.firstName = "El nombre debe tener entre 2 y 50 caracteres"

  const lastName = text(body, "lastName")
  if (!lastName) errors.lastName = "El apellido es obligatorio"
  else if (lastName.length < 2 || lastName.length > 50) errors.lastName = "El apellido debe tener entre 2 y 50 caracteres"

  const username = text(body, "username")
  if (!username) errors.username = "El nombre de usuario es obligatorio"
  else if (!USERNAME_PATTERN.test(username)) errors.username = USERNAME_MESSAGE

  const email = text(body, "email")
  if (!email) errors.email = "El correo electrónico es obligatorio"
  else if (!EMAIL_PATTERN.test(email) || email.length > 255) errors.email = "Debe proporcionar un correo electrónico válido"

  const password = textField(body, "password") ?? ""
  if (!password) errors.password = "La contraseña es obligatoria"
  else if (!PASSWORD_PATTERN.test(password)) errors.password = PASSWORD_MESSAGE

  const institution = text(body, "institution")
  if (institution.length > 255) errors.institution = "La institución no puede superar 255 caracteres"

  if (Object.keys(errors).length > 0) throw fieldErrors(errors)

  const { state } = ctx
  if (findUserByEmail(state, email)) {
    throw httpError(409, "El correo electrónico ya está registrado.", "error")
  }
  if (state.users.some((u) => u.username.toLowerCase() === username.toLowerCase())) {
    throw httpError(409, "El nombre de usuario ya está registrado.", "error")
  }

  const user: UserRecord = {
    id: newId(),
    firstName,
    lastName,
    username,
    email,
    institution: institution || null,
    role: "STUDENT",
    isActive: true,
    createdAt: isoAt(ctx.now),
    passwordDigest: digest(password),
    failedLogins: 0,
    lockedUntil: null,
  }
  state.users.push(user)
  return created({ message: "Usuario registrado exitosamente", userId: user.id })
}

function login(req: MockRequest, ctx: MockContext) {
  const body = readBody(req, "error")
  const email = text(body, "email")
  const password = textField(body, "password") ?? ""

  const errors: Record<string, string> = {}
  if (!email) errors.email = "El correo electrónico es obligatorio"
  else if (!EMAIL_PATTERN.test(email)) errors.email = "El correo electrónico debe ser válido"
  if (!password) errors.password = "La contraseña es obligatoria"
  if (Object.keys(errors).length > 0) throw fieldErrors(errors)

  const { state, now } = ctx
  const user = findUserByEmail(state, email)

  if (user && user.lockedUntil !== null && user.lockedUntil > now) {
    throw httpError(423, LOCKED_MESSAGE, "error")
  }

  if (!user || user.passwordDigest !== digest(password)) {
    if (user) {
      user.failedLogins += 1
      if (user.failedLogins >= LOGIN_MAX_FAILED_ATTEMPTS) {
        user.lockedUntil = now + LOGIN_LOCK_MS
        user.failedLogins = 0
        throw httpError(423, LOCKED_MESSAGE, "error")
      }
    }
    throw httpError(401, "Credenciales incorrectas", "error")
  }

  user.failedLogins = 0
  user.lockedUntil = null
  return ok(issueSession(state, user, now))
}

function refresh(req: MockRequest, ctx: MockContext) {
  const body = readBody(req, "error")
  const refreshToken = text(body, "refreshToken")
  if (!refreshToken) throw fieldErrors({ refreshToken: "El refresh token es obligatorio" })

  const { state, now } = ctx
  const record = state.refreshTokens.find((r) => r.token === refreshToken && r.expiresAt > now)
  const user = record ? state.users.find((u) => u.id === record.userId) : undefined
  if (!record || !user) throw httpError(401, "Refresh token inválido o expirado", "error")

  // Rotación: el refresh token usado deja de valer.
  state.refreshTokens = state.refreshTokens.filter((r) => r.token !== refreshToken)
  return ok(issueSession(state, user, now))
}

function checkUsername(req: MockRequest, ctx: MockContext) {
  const username = (req.query.get("username") ?? "").trim()
  if (!USERNAME_PATTERN.test(username)) throw httpError(400, USERNAME_MESSAGE, "error")

  let ownId: string | null = null
  if (req.authorization) {
    try {
      ownId = authenticate(ctx.state, req.authorization, ctx.now).id
    } catch {
      ownId = null
    }
  }
  const taken = ctx.state.users.some(
    (u) => u.username.toLowerCase() === username.toLowerCase() && u.id !== ownId
  )
  return ok({ available: !taken })
}

export function registerAuthRoutes(router: ApiRouter): void {
  router.add("POST", "/v1/auth/register", register)
  router.add("POST", "/v1/auth/login", login)
  router.add("POST", "/v1/auth/logout", (req, ctx) => {
    revokeSession(ctx.state, req.authorization)
    return ok()
  })
  router.add("POST", "/v1/auth/refresh", refresh)
  router.add("GET", "/v1/auth/me", (req, ctx) => {
    const user = authenticate(ctx.state, req.authorization, ctx.now)
    return ok(toUserProfile(user))
  })
  router.add("GET", "/v1/auth/check-username", checkUsername)
}
