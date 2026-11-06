/**
 * Prueba de humo del modo mock: recorre el flujo del estudiante sin backend ni navegador.
 *
 *   login → maratones → inscripción por contraseña → invitación → envío → veredicto
 *   (y además: errores 400/401/403/404/409/405, invitaciones, equipos, aclaraciones,
 *   ranking, registro, logout, persistencia y la fábrica createHttpApi con y sin producción).
 *
 * Uso (desde frontend/apps/student-app):   node scripts/mock-smoke.cjs
 *
 * Sin dependencias nuevas: transpila los .ts con el paquete `typescript` del proyecto y
 * resuelve el alias "@/" hacia src/. El reloj es inyectable, así que el veredicto se
 * avanza sin esperar.
 */
const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const Module = require("node:module")
const ts = require("typescript")

const SRC = path.join(__dirname, "..", "src")
const MOCK_DIR = path.join(SRC, "core", "api", "mock")

// 1. Cargar archivos .ts transpilándolos a CommonJS.
require.extensions[".ts"] = (module, filename) => {
  const { outputText } = ts.transpileModule(fs.readFileSync(filename, "utf8"), {
    fileName: filename,
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true },
  })
  module._compile(outputText, filename)
}

// 2. Resolver el alias "@/" hacia src/.
const originalResolve = Module._resolveFilename
Module._resolveFilename = function resolveWithAlias(request, ...rest) {
  if (request.startsWith("@/")) request = path.join(SRC, request.slice(2))
  return originalResolve.call(this, request, ...rest)
}

const { MockServer } = require(path.join(MOCK_DIR, "server.ts"))
const { DEMO_STUDENT_EMAIL, DEMO_STUDENT_PASSWORD, DEMO_CONTEST_PASSWORD } = require(path.join(MOCK_DIR, "model.ts"))
const { uid } = require(path.join(MOCK_DIR, "util.ts"))

const DEMO_ID = uid(1, 1)
const CARLOS_ID = uid(1, 3)
const LAURA_ID = uid(1, 4)
const CARLOS_EMAIL = "carlos.mendoza@uceva.edu.co"
const DEMO_TEAM_C1 = uid(6, 1) // "Los Algoritmos", ACTIVE, capitán = demo
const PROBLEM_C = uid(4, 3) // "Ventana deslizante máxima" (cp C de la maratón ACTIVE)
const PROBLEM_NOVATOS = uid(4, 6) // "Palíndromo más largo" (maratón UPCOMING)
const CP_A_C1 = uid(3, 11) // relación concurso-problema A de la maratón ACTIVE

let now = Date.now()
const server = new MockServer({ now: () => now, latency: null, storage: null })
const withToken = (token) => ({ headers: { Authorization: `Bearer ${token}` } })

let passed = 0
let failed = 0
async function step(name, fn) {
  try {
    await fn()
    passed++
    console.log(`  [ok]    ${name}`)
  } catch (error) {
    failed++
    process.exitCode = 1
    console.log(`  [fallo] ${name}\n          ${error.message.split("\n").join("\n          ")}`)
  }
}

async function judge(token, teamId, problemId, sourceCode) {
  const created = await server.post("/v1/submissions", { teamId, problemId, language: "PYTHON", sourceCode }, withToken(token))
  assert.equal(created.status, 201, `crear envío: ${JSON.stringify(created.data)}`)
  now += 6100
  const judged = await server.get(`/v1/submissions/${created.data.id}`)
  return judged.data
}

async function main() {
  console.log("Prueba de humo del modo mock\n")

  let token = ""
  let demoTeam = null
  let novatosTeam = null
  let contests = []
  const byTitle = (prefix) => contests.find((c) => c.title.startsWith(prefix))

  await step("login con contraseña incorrecta devuelve 401 con mensaje de auth-service", async () => {
    const res = await server.post("/v1/auth/login", { email: DEMO_STUDENT_EMAIL, password: "incorrecta" })
    assert.equal(res.status, 401)
    assert.equal(res.ok, false)
    assert.equal(res.error, "Credenciales incorrectas")
  })

  await step("login con datos inválidos devuelve 400 con errores por campo", async () => {
    const res = await server.post("/v1/auth/login", { email: "no-es-correo", password: "" })
    assert.equal(res.status, 400)
    assert.ok(res.data.email, "falta error de email")
    assert.ok(res.data.password, "falta error de password")
  })

  await step("login del estudiante demo emite un JWT falso bien formado", async () => {
    const res = await server.post("/v1/auth/login", { email: DEMO_STUDENT_EMAIL, password: DEMO_STUDENT_PASSWORD })
    assert.equal(res.status, 200)
    assert.equal(res.data.type, "Bearer")
    assert.equal(res.data.expiresIn, 15 * 60 * 1000)
    const parts = res.data.accessToken.split(".")
    assert.equal(parts.length, 3)
    assert.equal(parts[2], "mock-signature")
    const claims = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8"))
    assert.equal(claims.sub, DEMO_ID)
    assert.equal(claims.role, "STUDENT")
    assert.equal(claims.email, DEMO_STUDENT_EMAIL)
    assert.ok(Math.abs(claims.exp * 1000 - (now + 15 * 60 * 1000)) < 1000, "exp debe ser ~15 min")
    token = res.data.accessToken
  })

  await step("GET /v1/auth/me devuelve el perfil del usuario demo", async () => {
    const res = await server.get("/v1/auth/me", withToken(token))
    assert.equal(res.status, 200)
    assert.equal(res.data.username, "demo_student")
    assert.equal(res.data.id, DEMO_ID)
  })

  await step("GET /v1/auth/me sin token devuelve 401", async () => {
    const res = await server.get("/v1/auth/me")
    assert.equal(res.status, 401)
  })

  await step("listado de maratones: 1 ACTIVE, 2 UPCOMING y 2 ENDED", async () => {
    const res = await server.get("/v1/contests")
    assert.equal(res.status, 200)
    contests = res.data
    const count = (s) => contests.filter((c) => c.status === s).length
    assert.equal(count("ACTIVE"), 1)
    assert.equal(count("UPCOMING"), 2)
    assert.equal(count("ENDED"), 2)
  })

  await step("detalle de maratón inexistente devuelve 404 con cuerpo { message }", async () => {
    const res = await server.get(`/v1/contests/${uid(2, 999)}`)
    assert.equal(res.status, 404)
    assert.ok(res.data.message)
  })

  await step("inscripción con contraseña incorrecta devuelve 403", async () => {
    const c2 = byTitle("Copa Goslint Novatos")
    const res = await server.post(`/v1/contests/${c2.id}/teams`, { teamName: "Los Novatos", contestPassword: "mal" }, withToken(token))
    assert.equal(res.status, 403)
    assert.equal(res.error, "La contraseña del concurso no es correcta.")
  })

  await step("inscripción en la maratón UPCOMING con contraseña correcta crea el equipo (201)", async () => {
    const c2 = byTitle("Copa Goslint Novatos")
    const res = await server.post(
      `/v1/contests/${c2.id}/teams`,
      { teamName: "Los Algoritmos Novatos", contestPassword: DEMO_CONTEST_PASSWORD },
      withToken(token)
    )
    assert.equal(res.status, 201)
    assert.equal(res.data.captainId, DEMO_ID)
    assert.equal(res.data.members.length, 1)
    novatosTeam = res.data
  })

  await step("una segunda inscripción del mismo usuario en la maratón devuelve 409", async () => {
    const c2 = byTitle("Copa Goslint Novatos")
    const res = await server.post(
      `/v1/contests/${c2.id}/teams`,
      { teamName: "Otro Nombre", contestPassword: DEMO_CONTEST_PASSWORD },
      withToken(token)
    )
    assert.equal(res.status, 409)
  })

  await step("inscripción en la maratón ACTIVE devuelve 409 (inscripciones cerradas)", async () => {
    const c1 = byTitle("Maratón de Programación UCEVA 2026-II")
    const res = await server.post(
      `/v1/contests/${c1.id}/teams`,
      { teamName: "Equipo Tardío", contestPassword: DEMO_CONTEST_PASSWORD },
      withToken(token)
    )
    assert.equal(res.status, 409)
  })

  await step("el capitán invita a Carlos y la invitación queda PENDING", async () => {
    const res = await server.post(
      `/v1/teams/${novatosTeam.id}/invitations`,
      { userId: CARLOS_ID, email: CARLOS_EMAIL },
      withToken(token)
    )
    assert.equal(res.status, 201)
    assert.equal(res.data.status, "PENDING")
    assert.equal(res.data.invitedUserId, CARLOS_ID)
  })

  await step("un no capitán no puede invitar (403) y una invitación repetida da 409", async () => {
    const login = await server.post("/v1/auth/login", { email: CARLOS_EMAIL, password: DEMO_STUDENT_PASSWORD })
    const carlosToken = login.data.accessToken
    const forbidden = await server.post(
      `/v1/teams/${novatosTeam.id}/invitations`,
      { userId: LAURA_ID, email: "laura.restrepo@uceva.edu.co" },
      withToken(carlosToken)
    )
    assert.equal(forbidden.status, 403)

    const repeated = await server.post(
      `/v1/teams/${novatosTeam.id}/invitations`,
      { userId: CARLOS_ID, email: CARLOS_EMAIL },
      withToken(token)
    )
    assert.equal(repeated.status, 409)
  })

  await step("envío a la maratón ACTIVE: PENDING → QUEUED → JUDGING → JUDGED (ACCEPTED)", async () => {
    const code = "n, k = map(int, input().split())\nprint(n - k + 1)  # prueba de humo\n"
    const created = await server.post(
      "/v1/submissions",
      { teamId: DEMO_TEAM_C1, problemId: PROBLEM_C, language: "PYTHON", sourceCode: code },
      withToken(token)
    )
    assert.equal(created.status, 201)
    assert.equal(created.data.status, "PENDING")
    const id = created.data.id

    now += 1600
    assert.equal((await server.get(`/v1/submissions/${id}`)).data.status, "QUEUED")
    now += 1500
    assert.equal((await server.get(`/v1/submissions/${id}`)).data.status, "JUDGING")
    now += 3000
    const judged = (await server.get(`/v1/submissions/${id}`)).data
    assert.equal(judged.status, "JUDGED")
    assert.equal(judged.verdict, "ACCEPTED")
    assert.equal(typeof judged.executionTimeMs, "number")
    assert.equal(typeof judged.memoryUsedKb, "number")

    const duplicate = await server.post(
      "/v1/submissions",
      { teamId: DEMO_TEAM_C1, problemId: PROBLEM_C, language: "PYTHON", sourceCode: code },
      withToken(token)
    )
    assert.equal(duplicate.status, 409, "un envío idéntico debe dar 409")
  })

  await step("reglas del juez simulado: TODO → WRONG_ANSWER, while True → TLE, syntax_error → CE", async () => {
    assert.equal((await judge(token, DEMO_TEAM_C1, PROBLEM_C, "print(1)  # TODO")).verdict, "WRONG_ANSWER")
    const loop = "while True:\n    total = total + 1\n    print(total)\n"
    assert.equal((await judge(token, DEMO_TEAM_C1, PROBLEM_C, loop)).verdict, "TIME_LIMIT_EXCEEDED")
    assert.equal((await judge(token, DEMO_TEAM_C1, PROBLEM_C, "print('a') # syntax_error here")).verdict, "COMPILATION_ERROR")
  })

  await step("envío a una maratón no ACTIVE devuelve 403", async () => {
    const res = await server.post(
      "/v1/submissions",
      { teamId: novatosTeam.id, problemId: PROBLEM_NOVATOS, language: "PYTHON", sourceCode: "print('hola mundo')" },
      withToken(token)
    )
    assert.equal(res.status, 403)
    assert.match(res.error, /no está activo/)
  })

  await step("envío de quien no es integrante del equipo devuelve 403", async () => {
    const login = await server.post("/v1/auth/login", { email: CARLOS_EMAIL, password: DEMO_STUDENT_PASSWORD })
    const res = await server.post(
      "/v1/submissions",
      { teamId: DEMO_TEAM_C1, problemId: PROBLEM_C, language: "PYTHON", sourceCode: "print('no soy integrante')" },
      withToken(login.data.accessToken)
    )
    assert.equal(res.status, 403)
  })

  await step("el envío con lenguaje no admitido devuelve 400", async () => {
    const res = await server.post(
      "/v1/submissions",
      { teamId: DEMO_TEAM_C1, problemId: PROBLEM_C, language: "COBOL", sourceCode: "x" },
      withToken(token)
    )
    assert.equal(res.status, 400)
  })

  await step("el ranking de la maratón ACTIVE refleja los envíos aceptados", async () => {
    const c1 = byTitle("Maratón de Programación UCEVA 2026-II")
    const res = await server.get(`/v1/contests/${c1.id}/scoreboard`)
    assert.equal(res.status, 200)
    assert.equal(res.data[0].teamName, "Los Algoritmos")
    assert.equal(res.data[0].score, 450)
    demoTeam = res.data[0]
  })

  await step("el usuario ve sus invitaciones: 2 PENDING y 1 REJECTED", async () => {
    const res = await server.get("/v1/invitations/me", withToken(token))
    assert.equal(res.status, 200)
    assert.equal(res.data.filter((i) => i.status === "PENDING").length, 2)
    assert.equal(res.data.filter((i) => i.status === "REJECTED").length, 1)
  })

  await step("aceptar una invitación mete al usuario en el equipo; aceptarla otra vez da 409", async () => {
    const accept = await server.post(`/v1/invitations/${uid(8, 2)}/accept`, undefined, withToken(token))
    assert.equal(accept.status, 200)
    assert.ok(accept.data.members.some((m) => m.userId === DEMO_ID))
    const again = await server.post(`/v1/invitations/${uid(8, 2)}/accept`, undefined, withToken(token))
    assert.equal(again.status, 409)
  })

  await step("rechazar una invitación devuelve 204 y no se puede rechazar de nuevo", async () => {
    const rejected = await server.post(`/v1/invitations/${uid(8, 1)}/reject`, undefined, withToken(token))
    assert.equal(rejected.status, 204)
    const again = await server.post(`/v1/invitations/${uid(8, 1)}/reject`, undefined, withToken(token))
    assert.equal(again.status, 409)
  })

  await step("renombrar el equipo: nombre libre (200) y nombre repetido en la maratón (409)", async () => {
    const ok = await server.put(`/v1/teams/${novatosTeam.id}`, { teamName: "Algoritmos Novatos" }, withToken(token))
    assert.equal(ok.status, 200)
    assert.equal(ok.data.teamName, "Algoritmos Novatos")
    const clash = await server.put(`/v1/teams/${novatosTeam.id}`, { teamName: "Bugs Felices" }, withToken(token))
    assert.equal(clash.status, 409)
  })

  await step("aclaraciones: la maratón ACTIVE lista 2 y el equipo puede preguntar (201)", async () => {
    const c1 = byTitle("Maratón de Programación UCEVA 2026-II")
    const list = await server.get(`/v1/contests/${c1.id}/clarifications`)
    assert.equal(list.data.length, 2)
    const ask = await server.post(`/v1/contest-problems/${CP_A_C1}/clarifications`, { question: "¿Se admiten entradas vacías?" }, withToken(token))
    assert.equal(ask.status, 201)
    assert.equal(ask.data.status, "PENDING")
  })

  await step("un integrante ausente en el equipo no puede ser quitado (404)", async () => {
    const res = await server.delete(`/v1/teams/${novatosTeam.id}/members/${LAURA_ID}`, withToken(token))
    assert.equal(res.status, 404)
  })

  await step("registro: validación con 400, duplicado con 409 y alta correcta con 201", async () => {
    const invalid = await server.post("/v1/auth/register", {
      firstName: "A", lastName: "B", username: "x", email: "mal", password: "corta", institution: "",
    })
    assert.equal(invalid.status, 400)
    assert.ok(invalid.data.password && invalid.data.username && invalid.data.email)

    const dup = await server.post("/v1/auth/register", {
      firstName: "Nuevo", lastName: "Usuario", username: "nuevo_user", email: DEMO_STUDENT_EMAIL,
      password: "Clave123!", institution: "",
    })
    assert.equal(dup.status, 409)
    assert.equal(dup.error, "El correo electrónico ya está registrado.")

    const created = await server.post("/v1/auth/register", {
      firstName: "Nuevo", lastName: "Usuario", username: "nuevo_user", email: "nuevo@uceva.edu.co",
      password: "Clave123!", institution: "",
    })
    assert.equal(created.status, 201)

    const login = await server.post("/v1/auth/login", { email: "nuevo@uceva.edu.co", password: "Clave123!" })
    assert.equal(login.status, 200)
  })

  await step("cinco intentos fallidos bloquean la cuenta con 423", async () => {
    let last
    for (let i = 0; i < 5; i++) {
      last = await server.post("/v1/auth/login", { email: "nuevo@uceva.edu.co", password: "mal" + i })
    }
    assert.equal(last.status, 423)
    const during = await server.post("/v1/auth/login", { email: "nuevo@uceva.edu.co", password: "Clave123!" })
    assert.equal(during.status, 423)
    now += 16 * 60 * 1000
    const after = await server.post("/v1/auth/login", { email: "nuevo@uceva.edu.co", password: "Clave123!" })
    assert.equal(after.status, 200)
  })

  await step("logout revoca el token: la siguiente petición responde 401", async () => {
    // Token nuevo: el anterior ya caducó tras el salto de 16 min del bloqueo.
    const session = await server.post("/v1/auth/login", { email: DEMO_STUDENT_EMAIL, password: DEMO_STUDENT_PASSWORD })
    const fresh = session.data.accessToken
    assert.equal((await server.get("/v1/auth/me", withToken(fresh))).status, 200)
    const out = await server.post("/v1/auth/logout", undefined, withToken(fresh))
    assert.equal(out.status, 200)
    const res = await server.get("/v1/auth/me", withToken(fresh))
    assert.equal(res.status, 401)
    assert.equal(res.error, "Sesión cerrada o token revocado")
  })

  await step("ruta inexistente devuelve 404 y método no permitido devuelve 405", async () => {
    assert.equal((await server.get("/v1/no-existe")).status, 404)
    assert.equal((await server.delete("/v1/contests")).status, 405)
  })

  await step("el estado se conserva al recrear el servidor con el mismo almacenamiento", async () => {
    const mem = new Map()
    const storage = {
      getItem: (k) => (mem.has(k) ? mem.get(k) : null),
      setItem: (k, v) => mem.set(k, v),
      removeItem: (k) => mem.delete(k),
    }
    const first = new MockServer({ now: () => now, latency: null, storage })
    const login = await first.post("/v1/auth/login", { email: DEMO_STUDENT_EMAIL, password: DEMO_STUDENT_PASSWORD })
    const created = await first.post(
      "/v1/submissions",
      { teamId: DEMO_TEAM_C1, problemId: PROBLEM_C, language: "PYTHON", sourceCode: "print('persistencia de prueba')" },
      withToken(login.data.accessToken)
    )
    assert.equal(created.status, 201)
    const second = new MockServer({ now: () => now, latency: null, storage })
    const list = await second.get(`/v1/submissions/team/${DEMO_TEAM_C1}`)
    assert.ok(list.data.some((s) => s.id === created.data.id), "el envío debe seguir existiendo")
  })

  await step("createHttpApi: con mock activo devuelve el MockServer; en producción lo ignora", async () => {
    const httpClientPath = path.join(MOCK_DIR, "..", "http-client.ts")
    const loadFactory = (vars) => {
      const saved = { ...process.env }
      Object.assign(process.env, vars)
      for (const key of Object.keys(require.cache)) {
        if (key.startsWith(SRC)) delete require.cache[key]
      }
      try {
        return require(httpClientPath).createHttpApi()
      } finally {
        for (const key of Object.keys(process.env)) if (!(key in saved)) delete process.env[key]
        Object.assign(process.env, saved)
      }
    }
    const warnings = []
    const originalWarn = console.warn
    console.warn = (...args) => warnings.push(args.join(" "))
    try {
      // La caché de require se limpia entre casos, así que se compara por nombre de clase.
      const dev = loadFactory({ NEXT_PUBLIC_USE_MOCK: "true", NODE_ENV: "development" })
      assert.equal(dev.constructor.name, "MockServer", "en desarrollo debe devolver el MockServer")

      const prod = loadFactory({ NEXT_PUBLIC_USE_MOCK: "true", NODE_ENV: "production" })
      assert.notEqual(prod.constructor.name, "MockServer", "en producción no debe usarse el mock")
      assert.equal(prod.constructor.name, "HttpApiAdapter")
      assert.ok(warnings.some((w) => w.includes("producción")), "debe avisar en consola")

      const off = loadFactory({ NEXT_PUBLIC_USE_MOCK: "false", NODE_ENV: "development" })
      assert.equal(off.constructor.name, "HttpApiAdapter", "sin mock se usa el backend real")
    } finally {
      console.warn = originalWarn
    }
  })

  console.log(`\nResultado: ${passed} superados, ${failed} fallidos (de ${passed + failed}).`)
  if (novatosTeam && demoTeam) console.log(`Equipos de referencia: "${novatosTeam.teamName}" y "${demoTeam.teamName}".`)
}

main().catch((error) => {
  console.error("Error no controlado:", error)
  process.exitCode = 1
})
