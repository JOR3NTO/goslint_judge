/**
 * Pruebas de las utilidades de seguridad de sesión (sin dependencias nuevas, mismo
 * esquema que mock-smoke.cjs):
 *
 *   - core/auth/jwt.ts            decodificar `exp` y decidir si el token caducó
 *   - core/api/sanitize-error.ts  que ningún mensaje con JWT/Bearer/volcado llegue a la UI
 *   - core/auth/session.ts        refresh único compartido, reintento y cierre sin bucles
 *
 * Uso (desde frontend/apps/student-app):   node scripts/security-smoke.cjs
 */
const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const Module = require("node:module")
const ts = require("typescript")

const SRC = path.join(__dirname, "..", "src")

require.extensions[".ts"] = (module, filename) => {
  const { outputText } = ts.transpileModule(fs.readFileSync(filename, "utf8"), {
    fileName: filename,
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true },
  })
  module._compile(outputText, filename)
}
const originalResolve = Module._resolveFilename
Module._resolveFilename = function resolveWithAlias(request, ...rest) {
  if (request.startsWith("@/")) request = path.join(SRC, request.slice(2))
  return originalResolve.call(this, request, ...rest)
}

const b64url = (obj) => Buffer.from(JSON.stringify(obj)).toString("base64url")
const makeJwt = (claims) => `${b64url({ alg: "HS256", typ: "JWT" })}.${b64url(claims)}.firma-de-prueba`
const nowSec = () => Math.floor(Date.now() / 1000)

let passed = 0
let failed = 0
async function step(name, fn) {
  try {
    await fn()
    passed += 1
    console.log(`  [ok]    ${name}`)
  } catch (err) {
    failed += 1
    console.log(`  [fallo] ${name}\n          ${String(err && err.message).split("\n").join("\n          ")}`)
  }
}

// ─── Navegador simulado para session.ts ──────────────────────────────────────
function memoryStorage() {
  const map = new Map()
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
    clear: () => map.clear(),
  }
}
const assigned = []
globalThis.window = { location: { pathname: "/contests/arena/", assign: (url) => assigned.push(url) } }
globalThis.localStorage = memoryStorage()
globalThis.sessionStorage = memoryStorage()
let refreshCalls = 0
let refreshResponder = async () => ({ ok: false, status: 401, json: async () => ({ error: "Refresh token inválido o expirado" }) })
globalThis.fetch = async (url, init) => {
  if (String(url).endsWith("/v1/auth/refresh")) {
    refreshCalls += 1
    return refreshResponder(url, init)
  }
  throw new Error(`fetch inesperado: ${url}`)
}

const jwt = require(path.join(SRC, "core", "auth", "jwt.ts"))
const sanitize = require(path.join(SRC, "core", "api", "sanitize-error.ts"))
const session = require(path.join(SRC, "core", "auth", "session.ts"))

;(async () => {
  console.log("jwt.ts")
  await step("decodifica exp y detecta token vigente / caducado / dentro del margen", () => {
    assert.equal(jwt.getJwtExpiryMs(makeJwt({ exp: 1000 })), 1_000_000)
    assert.equal(jwt.isJwtExpired(makeJwt({ exp: nowSec() + 3600 })), false)
    assert.equal(jwt.isJwtExpired(makeJwt({ exp: nowSec() - 1 })), true)
    assert.equal(jwt.isJwtExpired(makeJwt({ exp: nowSec() + 10 })), true, "a 10 s de caducar se trata como caducado")
  })
  await step("token mal formado o sin exp se considera caducado", () => {
    assert.equal(jwt.decodeJwtPayload("no-es-un-jwt"), null)
    assert.equal(jwt.isJwtExpired("a.b.c"), true)
    assert.equal(jwt.isJwtExpired(makeJwt({ sub: "x" })), true)
  })

  console.log("sanitize-error.ts")
  const token = makeJwt({ sub: "u1", exp: nowSec() + 60 })
  await step("un JWT o un Bearer nunca pasan", () => {
    assert.equal(sanitize.sanitizeErrorMessage(`Token inválido: ${token}`, "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage(`Authorization=Bearer ${token}`, "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage("bearer.abcdefghijklmnop", "X"), "X")
  })
  await step("volcados crudos (JSON, HTML, trazas) se sustituyen por el genérico", () => {
    assert.equal(sanitize.sanitizeErrorMessage('{"password":"x","token":"y"}', "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage("<html><body>502</body></html>", "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage("refresh_token: abc", "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage(undefined, "X"), "X")
    assert.equal(sanitize.sanitizeErrorMessage("a".repeat(400), "X"), "X")
  })
  await step("los mensajes legítimos del backend se conservan", () => {
    const msg = "La contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial"
    assert.equal(sanitize.sanitizeErrorMessage(msg, "X"), msg)
    assert.equal(sanitize.sanitizeErrorMessage("Credenciales incorrectas", "X"), "Credenciales incorrectas")
    const notFound = "Ruta no disponible en el modo demo: GET /v1/contests/123e4567-e89b-12d3-a456-426614174000/clarifications"
    assert.equal(sanitize.sanitizeErrorMessage(notFound, "X"), notFound)
  })
  await step("sanitizeErrorBody: solo textos seguros de primer nivel; sin claves de credenciales", () => {
    const clean = sanitize.sanitizeErrorBody(
      { password: "Contraseña débil", accessToken: token, message: `x ${token}`, nested: { a: 1 }, count: 3 },
      400
    )
    assert.deepEqual(clean, { password: "Contraseña débil", message: sanitize.genericMessageForStatus(400) })
    assert.equal(sanitize.sanitizeErrorBody("texto", 500), null)
    assert.equal(sanitize.sanitizeErrorBody([1, 2], 500), null)
  })

  console.log("session.ts")
  const reset = () => {
    localStorage.clear()
    sessionStorage.clear()
    assigned.length = 0
    refreshCalls = 0
    window.location.pathname = "/contests/arena/"
  }

  await step("token vigente se usa sin llamar a refresh", async () => {
    reset()
    const valid = makeJwt({ sub: "u1", exp: nowSec() + 3600 })
    session.storeTokens(valid, "r1")
    assert.equal(await session.getValidAccessToken(), valid)
    assert.equal(refreshCalls, 0)
  })

  await step("token caducado: peticiones concurrentes comparten UN refresh y reciben el token nuevo", async () => {
    reset()
    const fresh = makeJwt({ sub: "u1", exp: nowSec() + 3600 })
    refreshResponder = async () => {
      await new Promise((r) => setTimeout(r, 20))
      return { ok: true, status: 200, json: async () => ({ accessToken: fresh, refreshToken: "r2", type: "Bearer" }) }
    }
    session.storeTokens(makeJwt({ sub: "u1", exp: nowSec() - 60 }), "r1")
    const results = await Promise.all([1, 2, 3, 4].map(() => session.getValidAccessToken()))
    assert.deepEqual(results, [fresh, fresh, fresh, fresh])
    assert.equal(refreshCalls, 1)
    assert.equal(localStorage.getItem("refresh_token"), "r2")
    assert.equal(assigned.length, 0)
  })

  await step("refresh rechazado: se borran tokens, se deja el aviso y se redirige una sola vez", async () => {
    reset()
    refreshResponder = async () => ({ ok: false, status: 401, json: async () => ({}) })
    session.storeTokens(makeJwt({ sub: "u1", exp: nowSec() - 60 }), "r1")
    const results = await Promise.all([session.getValidAccessToken(), session.getValidAccessToken()])
    assert.deepEqual(results, [null, null])
    assert.equal(localStorage.getItem("auth_token"), null)
    assert.equal(localStorage.getItem("refresh_token"), null)
    assert.deepEqual(assigned, ["/login"])
    assert.equal(session.consumeSessionNotice(), session.SESSION_EXPIRED_MESSAGE)
    assert.equal(session.consumeSessionNotice(), null, "el aviso es de un solo uso")
  })

  await step("en /login no se redirige (sin bucles)", async () => {
    reset()
    session.storeTokens(makeJwt({ sub: "u1", exp: nowSec() + 3600 }), "r1") // también rearma el redirect
    window.location.pathname = "/login/"
    session.expireSession()
    assert.deepEqual(assigned, [])
    assert.equal(localStorage.getItem("auth_token"), null)
  })

  await step("visitante sin sesión: null sin refresh ni redirección", async () => {
    reset()
    assert.equal(await session.getValidAccessToken(), null)
    assert.equal(refreshCalls, 0)
    assert.deepEqual(assigned, [])
  })

  await step("isSessionFailure: 401 siempre; 403 solo con token caducado", () => {
    const valid = makeJwt({ exp: nowSec() + 3600 })
    const expired = makeJwt({ exp: nowSec() - 5 })
    assert.equal(session.isSessionFailure(401, valid), true)
    assert.equal(session.isSessionFailure(403, valid), false)
    assert.equal(session.isSessionFailure(403, expired), true)
    assert.equal(session.isSessionFailure(500, expired), false)
  })

  console.log("http-client.ts")
  const { HttpApiAdapter } = require(path.join(SRC, "core", "api", "http-client.ts"))
  await step("401 con token: renueva una vez, reintenta y la respuesta de error nunca trae el token", async () => {
    reset()
    const first = makeJwt({ sub: "u1", exp: nowSec() + 3600 })
    const second = makeJwt({ sub: "u1", exp: nowSec() + 7200 })
    session.storeTokens(first, "r1")
    refreshResponder = async () => ({ ok: true, status: 200, json: async () => ({ accessToken: second, refreshToken: "r2" }) })
    const seen = []
    const previousFetch = globalThis.fetch
    globalThis.fetch = async (url, init) => {
      if (String(url).endsWith("/v1/auth/refresh")) return previousFetch(url, init)
      seen.push(init.headers.Authorization)
      if (init.headers.Authorization === `Bearer ${first}`) {
        return { ok: false, status: 401, statusText: "Unauthorized", json: async () => ({ error: `Token ${first} revocado` }) }
      }
      return { ok: true, status: 200, statusText: "OK", json: async () => ({ hello: "world" }) }
    }
    try {
      const api = new HttpApiAdapter("http://api.test/api")
      const res = await api.get("/v1/teams/user/u1")
      assert.equal(res.ok, true)
      assert.deepEqual(seen, [`Bearer ${first}`, `Bearer ${second}`])
      assert.equal(refreshCalls, 1)
      assert.deepEqual(assigned, [])

      // Error con el token en el cuerpo: se sanea antes de llegar a la feature.
      globalThis.fetch = async () => ({
        ok: false, status: 400, statusText: "Bad Request",
        json: async () => ({ password: `inválida ${second}`, message: `Authorization: Bearer ${second}` }),
      })
      const bad = await api.post("/v1/contests/c1/teams", { teamName: "x" })
      assert.equal(JSON.stringify(bad).includes("eyJ"), false)
      assert.equal(bad.error, sanitize.genericMessageForStatus(400))
    } finally {
      globalThis.fetch = previousFetch
    }
  })

  console.log(`\nResultado: ${passed} superados, ${failed} fallidos (de ${passed + failed}).`)
  process.exit(failed > 0 ? 1 : 0)
})()
