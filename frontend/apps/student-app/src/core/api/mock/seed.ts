/**
 * Datos semilla del modo demo.
 *
 * Todas las fechas se calculan a partir de `now` al sembrar, así que las maratones
 * quedan UPCOMING / ACTIVE / ENDED según la hora real. El estado de cada una se
 * recalcula en cada respuesta (ver domain.ts), por lo que los datos siguen coherentes
 * aunque la sesión dure más de lo previsto.
 *
 * Resumen de la demo:
 *  - C1 "Maratón de Programación UCEVA 2026-II": ACTIVE, 5 problemas, el estudiante
 *    demo ya está inscrito con "Los Algoritmos" (capitán, con Ana María Gómez).
 *  - C2 "Copa Goslint Novatos": UPCOMING, contraseña "maraton123", el estudiante NO
 *    está inscrito. Tiene una invitación pendiente de "Tigres de Java".
 *  - C3 "Reto Relámpago de Algoritmos": UPCOMING, contraseña "reto-secreto-2026".
 *    Invitación pendiente de "Reto Silicio".
 *  - C4 y C5: ENDED con equipos y ranking.
 */
import type { InvitationDTO, ProblemDTO, TeamMemberDTO } from "@/features/contests/types/contest.types"
import type { SampleDTO, SubmissionLanguage, SubmissionVerdict } from "@/features/arena/types/arena.types"
import type { UserRole } from "@/features/profile/types/profile.types"
import {
  DEMO_CONTEST_PASSWORD,
  DEMO_STUDENT_EMAIL,
  MOCK_SCHEMA_VERSION,
  SEED_USER_PASSWORD,
  type ClarificationRecord,
  type ContestProblemRecord,
  type ContestRecord,
  type MockState,
  type SubmissionRecord,
  type TeamRecord,
  type UserRecord,
} from "./model"
import { digest, isoAt, uid } from "./util"

const MIN = 60_000
const HOUR = 60 * MIN
const DAY = 24 * HOUR

// ─── Código de ejemplo (solo para mostrar en la UI; el veredicto de los semilla es fijo) ─

const CODE_SUMA = [
  "n, k = map(int, input().split())",
  "a = list(map(int, input().split()))",
  "visto = {}",
  "total = 0",
  "for x in a:",
  "    total += visto.get(k - x, 0)",
  "    visto[x] = visto.get(x, 0) + 1",
  "print(total)",
].join("\n")

const CODE_SUMA_INCOMPLETO = [
  "n, k = map(int, input().split())",
  "a = list(map(int, input().split()))",
  "# TODO: contar los pares sin repetir",
  "print(0)",
].join("\n")

const CODE_BFS = [
  "from collections import deque",
  "R, C = map(int, input().split())",
  "g = [input() for _ in range(R)]",
  "for r in range(R):",
  "    for c in range(C):",
  "        if g[r][c] == 'S': s = (r, c)",
  "        if g[r][c] == 'E': e = (r, c)",
  "dist = {s: 0}",
  "q = deque([s])",
  "while q:",
  "    r, c = q.popleft()",
  "    for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):",
  "        nr, nc = r + dr, c + dc",
  "        if 0 <= nr < R and 0 <= nc < C and g[nr][nc] != '#' and (nr, nc) not in dist:",
  "            dist[(nr, nc)] = dist[(r, c)] + 1",
  "            q.append((nr, nc))",
  "print(dist.get(e, -1))",
].join("\n")

const CODE_BFS_INCOMPLETO = [
  "from collections import deque",
  "# TODO: completar la búsqueda en anchura",
  "print(-1)",
].join("\n")

const CODE_GENERICO = [
  "import sys",
  "datos = sys.stdin.read().split()",
  "print(datos[0] if datos else 0)",
].join("\n")

// ─── Problemas (formato problem-service: enunciado en texto plano) ───────────

const PROBLEMS: ProblemDTO[] = [
  {
    id: uid(4, 1),
    title: "Suma de pares",
    statement: [
      "Te dan una lista de n enteros y un número objetivo K. Debes contar cuántos pares de posiciones distintas (i < j) cumplen que a[i] + a[j] = K.",
      "Es el calentamiento de la maratón: con una tabla de frecuencias puedes resolverlo en tiempo lineal.",
    ].join("\n\n"),
    timeLimitMs: 1000,
    memoryLimitKb: 131072,
    difficult: 800,
    inputFormat:
      "La primera línea contiene dos enteros n y K (1 ≤ n ≤ 200 000, |K| ≤ 10^9). La segunda línea contiene n enteros a[1], …, a[n] (|a[i]| ≤ 10^9).",
    outputFormat: "Imprime un único entero: la cantidad de pares (i, j) con i < j y a[i] + a[j] = K.",
  },
  {
    id: uid(4, 2),
    title: "Camino más corto en una cuadrícula",
    statement: [
      "Una cuadrícula de R filas y C columnas representa un mapa. Cada celda es libre ('.'), un muro ('#'), la posición inicial ('S') o la meta ('E').",
      "En cada movimiento puedes avanzar a una celda vecina en horizontal o en vertical, siempre que no sea un muro. Calcula la menor cantidad de movimientos necesarios para llegar de S a E.",
    ].join("\n\n"),
    timeLimitMs: 1000,
    memoryLimitKb: 131072,
    difficult: 1200,
    inputFormat:
      "La primera línea contiene R y C (1 ≤ R, C ≤ 1000). Las siguientes R líneas describen la cuadrícula, con exactamente una 'S' y una 'E'.",
    outputFormat: "Imprime la mínima cantidad de movimientos, o -1 si no existe un camino.",
  },
  {
    id: uid(4, 3),
    title: "Ventana deslizante máxima",
    statement: [
      "Dado un arreglo de n enteros y un tamaño de ventana k, para cada posición en la que la ventana de k elementos consecutivos cabe completa en el arreglo, reporta el valor máximo dentro de ella.",
      "No recorras cada ventana por completo: una cola doble que mantiene índices con valores decrecientes resuelve el problema en tiempo lineal.",
    ].join("\n\n"),
    timeLimitMs: 1000,
    memoryLimitKb: 131072,
    difficult: 1500,
    inputFormat:
      "La primera línea contiene n y k (1 ≤ k ≤ n ≤ 100 000). La segunda línea contiene n enteros (|a[i]| ≤ 10^9).",
    outputFormat: "Imprime n − k + 1 enteros separados por espacios: el máximo de cada ventana, de izquierda a derecha.",
  },
  {
    id: uid(4, 4),
    title: "Subsecuencia creciente más larga",
    statement: [
      "Una subsecuencia se obtiene eliminando cero o más elementos de un arreglo sin cambiar el orden de los restantes. Se pide la longitud de la subsecuencia estrictamente creciente más larga.",
      "La solución cuadrática no alcanza para n grande: busca una técnica con búsqueda binaria sobre las colas de las subsecuencias.",
    ].join("\n\n"),
    timeLimitMs: 2000,
    memoryLimitKb: 131072,
    difficult: 1900,
    inputFormat:
      "La primera línea contiene n (1 ≤ n ≤ 200 000). La segunda línea contiene n enteros (|a[i]| ≤ 10^9).",
    outputFormat: "Imprime la longitud de la subsecuencia estrictamente creciente más larga.",
  },
  {
    id: uid(4, 5),
    title: "Cableado de la ciudad",
    statement: [
      "La alcaldía quiere conectar todas las sedes universitarias de la ciudad con cable de fibra. Hay N sedes numeradas de 1 a N y M posibles tramos de cable, cada uno con su costo.",
      "Debes elegir un conjunto de tramos que conecte todas las sedes con el menor costo total. Si no es posible conectarlas todas, el resultado es -1.",
    ].join("\n\n"),
    timeLimitMs: 2000,
    memoryLimitKb: 262144,
    difficult: 2300,
    inputFormat:
      "La primera línea contiene N y M (1 ≤ N ≤ 100 000, 0 ≤ M ≤ 200 000). Cada una de las M líneas siguientes contiene u, v y w (1 ≤ u, v ≤ N, 1 ≤ w ≤ 10^9).",
    outputFormat: "Imprime el costo mínimo total, o -1 si no todas las sedes quedan conectadas.",
  },
  {
    id: uid(4, 6),
    title: "Palíndromo más largo",
    statement: [
      "Dada una cadena s de letras minúsculas, encuentra la longitud de la subcadena contigua más larga que se lee igual de izquierda a derecha que de derecha a izquierda.",
      "Puede haber varias respuestas con la misma longitud; basta con reportar la longitud.",
    ].join("\n\n"),
    timeLimitMs: 1000,
    memoryLimitKb: 131072,
    difficult: 1000,
    inputFormat: "Una única línea con la cadena s (1 ≤ |s| ≤ 1000), formada solo por letras minúsculas.",
    outputFormat: "Imprime la longitud del palíndromo contiguo más largo.",
  },
  {
    id: uid(4, 7),
    title: "Paréntesis balanceados",
    statement: [
      "Una cadena está balanceada si cada '(' tiene un ')' que lo cierra, respetando el orden de anidamiento.",
      "Dada una cadena formada solo por '(' y ')', determina la cantidad mínima de paréntesis que hay que insertar, en cualquier posición, para que quede balanceada. No es necesario reconstruir la cadena resultante.",
    ].join("\n\n"),
    timeLimitMs: 1000,
    memoryLimitKb: 131072,
    difficult: 1100,
    inputFormat:
      "Una única línea con la cadena s (0 ≤ |s| ≤ 100 000) formada por '(' y ')'. Si la cadena está vacía, la línea estará vacía.",
    outputFormat: "Imprime el número mínimo de paréntesis que hay que insertar.",
  },
  {
    id: uid(4, 8),
    title: "Mochila de caramelos",
    statement: [
      "Una tienda tiene n tipos de caramelos. Cada tipo tiene un peso w y un valor v. Tienes una mochila que soporta un peso máximo W y, de cada tipo, puedes llevar a lo sumo una unidad.",
      "Determina el valor máximo total que puedes llevar sin superar el peso W.",
    ].join("\n\n"),
    timeLimitMs: 2000,
    memoryLimitKb: 131072,
    difficult: 2600,
    inputFormat:
      "La primera línea contiene n y W (1 ≤ n ≤ 100, 1 ≤ W ≤ 10 000). Cada una de las n líneas siguientes contiene w y v (1 ≤ w ≤ 10 000, 0 ≤ v ≤ 10^9).",
    outputFormat: "Imprime el valor máximo alcanzable.",
  },
]

/** Casos de muestra por problema (índice 1-based igual que `uid(4, n)`). */
const SAMPLES: Record<number, Array<[string, string]>> = {
  1: [
    ["5 6\n1 5 3 3 7\n", "2\n"],
    ["3 10\n1 2 3\n", "0\n"],
  ],
  2: [
    ["3 4\nS..#\n.#..\n...E\n", "5\n"],
    ["2 2\nS#\n#E\n", "-1\n"],
  ],
  3: [
    ["6 3\n1 3 -1 -3 5 3\n", "3 3 5 5\n"],
    ["4 1\n2 9 4 7\n", "2 9 4 7\n"],
  ],
  4: [
    ["6\n10 9 2 5 3 7\n", "3\n"],
    ["5\n1 2 2 2 3\n", "3\n"],
  ],
  5: [
    ["4 5\n1 2 1\n2 3 2\n3 4 3\n1 3 4\n2 4 5\n", "6\n"],
    ["3 1\n1 2 7\n", "-1\n"],
  ],
  6: [
    ["babad\n", "3\n"],
    ["cbbd\n", "2\n"],
  ],
  7: [
    ["())\n", "1\n"],
    ["(((\n", "3\n"],
  ],
  8: [
    ["3 5\n2 3\n3 4\n4 5\n", "7\n"],
    ["2 1\n2 10\n3 20\n", "0\n"],
  ],
}

// ─── Usuarios ────────────────────────────────────────────────────────────────

const USER_SPECS: Array<{
  n: number
  firstName: string
  lastName: string
  username: string
  email: string
  role: UserRole
  institution: string | null
}> = [
  { n: 1, firstName: "Estudiante", lastName: "Demo", username: "demo_student", email: DEMO_STUDENT_EMAIL, role: "STUDENT", institution: "Unidad Central del Valle del Cauca" },
  { n: 2, firstName: "Ana María", lastName: "Gómez", username: "ana_gomez", email: "ana.gomez@uceva.edu.co", role: "STUDENT", institution: "Unidad Central del Valle del Cauca" },
  { n: 3, firstName: "Carlos", lastName: "Mendoza", username: "carlos_mendoza", email: "carlos.mendoza@uceva.edu.co", role: "STUDENT", institution: "Unidad Central del Valle del Cauca" },
  { n: 4, firstName: "Laura", lastName: "Restrepo", username: "laura_restrepo", email: "laura.restrepo@uceva.edu.co", role: "STUDENT", institution: "Unidad Central del Valle del Cauca" },
  { n: 5, firstName: "Mateo", lastName: "Ospina", username: "mateo_ospina", email: "mateo.ospina@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 6, firstName: "Sofía", lastName: "Ríos", username: "sofia_rios", email: "sofia.rios@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 7, firstName: "Andrés", lastName: "Pineda", username: "andres_pineda", email: "andres.pineda@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 8, firstName: "Valentina", lastName: "Cruz", username: "vale_cruz", email: "valentina.cruz@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 9, firstName: "Luis", lastName: "Herrera", username: "luis_herrera", email: "luis.herrera@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 10, firstName: "Daniela", lastName: "Ocampo", username: "daniela_ocampo", email: "daniela.ocampo@uceva.edu.co", role: "STUDENT", institution: null },
  { n: 11, firstName: "Coordinación", lastName: "UCEVA", username: "coordinacion_uceva", email: "coordinacion@uceva.edu.co", role: "ADMIN", institution: "Unidad Central del Valle del Cauca" },
]

// ─── Sembrado ────────────────────────────────────────────────────────────────

/** Crea el estado inicial completo. Puro: depende solo de `now`. */
export function createSeedState(now: number): MockState {
  const passwordDigest = digest(SEED_USER_PASSWORD)

  const users: UserRecord[] = USER_SPECS.map((u) => ({
    id: uid(1, u.n),
    firstName: u.firstName,
    lastName: u.lastName,
    username: u.username,
    email: u.email,
    institution: u.institution,
    role: u.role,
    isActive: true,
    createdAt: isoAt(now - (60 - u.n) * DAY),
    passwordDigest,
    failedLogins: 0,
    lockedUntil: null,
  }))

  // Maratones: C1 ACTIVE, C2 y C3 UPCOMING, C4 y C5 ENDED.
  const c1Start = now - 60 * MIN
  const c4Start = now - 30 * DAY
  const c5Start = now - 10 * DAY
  const contestSpecs: Array<{ n: number; title: string; description: string; start: number; end: number; password: string }> = [
    {
      n: 1,
      title: "Maratón de Programación UCEVA 2026-II",
      description:
        "Primera fecha del semestre. Equipos de hasta tres integrantes resuelven problemas al estilo ICPC: gana quien resuelva más problemas con menor penalización.",
      start: c1Start,
      end: now + 120 * MIN,
      password: DEMO_CONTEST_PASSWORD,
    },
    {
      n: 2,
      title: "Copa Goslint Novatos",
      description: "Maratón de iniciación para estudiantes de primeros semestres. No se requiere experiencia previa.",
      start: now + 24 * HOUR,
      end: now + 27 * HOUR,
      password: DEMO_CONTEST_PASSWORD,
    },
    {
      n: 3,
      title: "Reto Relámpago de Algoritmos",
      description: "Dos horas de problemas cortos de grafos y programación dinámica.",
      start: now + 72 * HOUR,
      end: now + 74 * HOUR,
      password: "reto-secreto-2026",
    },
    {
      n: 4,
      title: "Maratón de Programación UCEVA 2026-I",
      description: "Edición anterior de la maratón institucional.",
      start: c4Start,
      end: c4Start + 5 * HOUR,
      password: "uceva-2026-1",
    },
    {
      n: 5,
      title: "Copa Goslint de Otoño 2026",
      description: "Copa de entrenamiento con tres problemas y ranking final.",
      start: c5Start,
      end: c5Start + 3 * HOUR,
      password: "otono-2026",
    },
  ]

  const contests: ContestRecord[] = contestSpecs.map((c) => ({
    id: uid(2, c.n),
    createdBy: uid(1, 11),
    title: c.title,
    description: c.description,
    startTime: isoAt(c.start),
    endTime: isoAt(c.end),
    createdAt: isoAt(c.start - 14 * DAY),
    passwordDigest: digest(c.password),
  }))

  const contestStart = (n: number) => contestSpecs[n - 1].start

  // Problemas por maratón (índice = problemN, letra = posición).
  const CONTEST_PROBLEMS: Array<{ contest: number; problems: number[]; points: number[] }> = [
    { contest: 1, problems: [1, 2, 3, 4, 5], points: [100, 150, 200, 250, 300] },
    { contest: 2, problems: [6, 7, 1], points: [100, 150, 200] },
    { contest: 3, problems: [2, 8], points: [150, 300] },
    { contest: 4, problems: [3, 4, 6], points: [150, 200, 100] },
    { contest: 5, problems: [7, 8, 1], points: [100, 300, 150] },
  ]
  const BALLOONS = ["red", "blue", "yellow", "green", "orange"]
  const LETTERS = ["A", "B", "C", "D", "E"]

  const contestProblems: ContestProblemRecord[] = []
  for (const spec of CONTEST_PROBLEMS) {
    spec.problems.forEach((problemN, idx) => {
      contestProblems.push({
        id: uid(3, spec.contest * 10 + idx + 1),
        contestId: uid(2, spec.contest),
        problemId: uid(4, problemN),
        alias: LETTERS[idx],
        orderIndex: idx,
        points: spec.points[idx],
        penalty: 20,
        balloonColor: BALLOONS[idx],
      })
    })
  }
  const cpId = (contest: number, idx: number) => uid(3, contest * 10 + idx + 1)

  const problems: ProblemDTO[] = PROBLEMS.map((p) => ({ ...p }))

  const samples: SampleDTO[] = []
  Object.entries(SAMPLES).forEach(([problemKey, cases]) => {
    const problemN = Number(problemKey)
    cases.forEach(([input, output], i) => {
      samples.push({
        id: uid(5, problemN * 10 + i + 1),
        problemId: uid(4, problemN),
        input,
        output,
        expectedOutput: output,
        orderIndex: i,
        isSample: true,
      })
    })
  })

  // Equipos. El estudiante demo es el capitán de "Los Algoritmos" en la maratón ACTIVE.
  const TEAM_SPECS: Array<{ n: number; contest: number; captain: number; name: string; members: number[]; createdAt: number }> = [
    { n: 1, contest: 1, captain: 1, name: "Los Algoritmos", members: [1, 2], createdAt: c1Start - 2 * HOUR },
    { n: 2, contest: 1, captain: 5, name: "Bits Rebeldes", members: [5, 6, 7], createdAt: c1Start - 2 * HOUR },
    { n: 3, contest: 1, captain: 8, name: "Nodo Cero", members: [8, 9], createdAt: c1Start - 90 * MIN },
    { n: 4, contest: 1, captain: 3, name: "Compiladores Unidos", members: [3, 4], createdAt: c1Start - 90 * MIN },
    { n: 5, contest: 2, captain: 6, name: "Tigres de Java", members: [6, 7], createdAt: now - 3 * HOUR },
    { n: 6, contest: 2, captain: 9, name: "Bugs Felices", members: [9], createdAt: now - 2 * HOUR },
    { n: 7, contest: 3, captain: 4, name: "Reto Silicio", members: [4], createdAt: now - 5 * HOUR },
    { n: 8, contest: 4, captain: 5, name: "Algoritmia Pro", members: [5, 8], createdAt: c4Start - DAY },
    { n: 9, contest: 4, captain: 9, name: "Pythonistas", members: [9, 3], createdAt: c4Start - DAY },
    { n: 10, contest: 4, captain: 10, name: "Cero Bugs", members: [10], createdAt: c4Start - DAY },
    { n: 11, contest: 5, captain: 7, name: "Byte Coders", members: [7, 5], createdAt: c5Start - DAY },
    { n: 12, contest: 5, captain: 8, name: "Stack Overflowers", members: [8], createdAt: c5Start - DAY },
  ]

  const teams: TeamRecord[] = TEAM_SPECS.map((t) => {
    const teamId = uid(6, t.n)
    const members: TeamMemberDTO[] = t.members.map((userN, idx) => ({
      id: uid(7, t.n * 10 + idx + 1),
      teamId,
      userId: uid(1, userN),
      registeredAt: isoAt(t.createdAt + (idx + 1) * MIN),
    }))
    return {
      id: teamId,
      contestId: uid(2, t.contest),
      captainId: uid(1, t.captain),
      teamName: t.name,
      createdAt: isoAt(t.createdAt),
      members,
    }
  })

  // Envíos semilla. Los veredictos son fijos; los minutos cuentan desde el inicio de la maratón.
  const submissions: SubmissionRecord[] = []
  let subN = 0
  const addSub = (
    contest: number,
    teamN: number,
    problemN: number,
    minute: number,
    verdict: SubmissionVerdict,
    code: string,
    language: SubmissionLanguage = "PYTHON"
  ) => {
    subN++
    submissions.push({
      id: uid(9, subN),
      teamId: uid(6, teamN),
      problemId: contestProblems.find((cp) => cp.contestId === uid(2, contest) && cp.problemId === uid(4, problemN))!.id,
      language,
      sourceCode: code,
      submittedAt: isoAt(contestStart(contest) + minute * MIN),
      verdictOverride: verdict,
    })
  }

  // C1 (ACTIVE). Demo: A AC 12', B WA 20' y AC 35', C WA 50'.
  addSub(1, 1, 1, 12, "ACCEPTED", CODE_SUMA)
  addSub(1, 1, 2, 20, "WRONG_ANSWER", CODE_BFS_INCOMPLETO)
  addSub(1, 1, 2, 35, "ACCEPTED", CODE_BFS)
  addSub(1, 1, 3, 50, "WRONG_ANSWER", CODE_SUMA_INCOMPLETO)
  // Bits Rebeldes: A AC 8', C AC 30', E WA 40'.
  addSub(1, 2, 1, 8, "ACCEPTED", CODE_SUMA)
  addSub(1, 2, 3, 30, "ACCEPTED", CODE_GENERICO)
  addSub(1, 2, 5, 40, "WRONG_ANSWER", CODE_SUMA_INCOMPLETO)
  // Nodo Cero: A WA 5' y AC 25', B AC 44'.
  addSub(1, 3, 1, 5, "WRONG_ANSWER", CODE_SUMA_INCOMPLETO)
  addSub(1, 3, 1, 25, "ACCEPTED", CODE_SUMA)
  addSub(1, 3, 2, 44, "ACCEPTED", CODE_BFS)
  // Compiladores Unidos: A AC 18', D TLE 60'.
  addSub(1, 4, 1, 18, "ACCEPTED", CODE_SUMA)
  addSub(1, 4, 4, 60, "TIME_LIMIT_EXCEEDED", "while True:\n    pass")

  // C4 (ENDED).
  addSub(4, 8, 3, 40, "ACCEPTED", CODE_GENERICO)
  addSub(4, 8, 4, 70, "WRONG_ANSWER", CODE_SUMA_INCOMPLETO)
  addSub(4, 8, 4, 95, "ACCEPTED", CODE_GENERICO)
  addSub(4, 9, 3, 25, "ACCEPTED", CODE_GENERICO)
  addSub(4, 9, 6, 30, "ACCEPTED", CODE_GENERICO)
  addSub(4, 9, 4, 100, "TIME_LIMIT_EXCEEDED", "while True:\n    pass")
  addSub(4, 10, 6, 120, "ACCEPTED", CODE_GENERICO)
  // C5 (ENDED).
  addSub(5, 11, 7, 15, "ACCEPTED", CODE_GENERICO)
  addSub(5, 11, 8, 120, "WRONG_ANSWER", CODE_SUMA_INCOMPLETO)
  addSub(5, 11, 1, 50, "ACCEPTED", CODE_SUMA)
  addSub(5, 12, 8, 150, "ACCEPTED", CODE_GENERICO)

  // Invitaciones: 2 PENDING para el estudiante demo y 1 REJECTED de una maratón anterior.
  const invitations: InvitationDTO[] = [
    {
      id: uid(8, 1),
      teamId: uid(6, 5),
      invitedUserId: uid(1, 1),
      invitedEmail: DEMO_STUDENT_EMAIL,
      status: "PENDING",
      createdAt: isoAt(now - 2 * HOUR),
      respondedAt: null,
    },
    {
      id: uid(8, 2),
      teamId: uid(6, 7),
      invitedUserId: uid(1, 1),
      invitedEmail: DEMO_STUDENT_EMAIL,
      status: "PENDING",
      createdAt: isoAt(now - 30 * MIN),
      respondedAt: null,
    },
    {
      id: uid(8, 3),
      teamId: uid(6, 10),
      invitedUserId: uid(1, 1),
      invitedEmail: DEMO_STUDENT_EMAIL,
      status: "REJECTED",
      createdAt: isoAt(c4Start - 2 * DAY),
      respondedAt: isoAt(c4Start - 2 * DAY + HOUR),
    },
  ]

  const clarifications: ClarificationRecord[] = [
    {
      id: uid(10, 1),
      contestProblemId: cpId(1, 2),
      teamId: uid(6, 2),
      question: "¿Si k es igual a n, debe imprimirse una sola ventana?",
      answer: "Sí. Cuando k = n solo existe una ventana y se imprime su máximo.",
      status: "ANSWERED",
      createdAt: isoAt(c1Start + 40 * MIN),
      answeredAt: isoAt(c1Start + 55 * MIN),
    },
    {
      id: uid(10, 2),
      contestProblemId: cpId(1, 1),
      teamId: uid(6, 1),
      question: "¿El mapa siempre tiene exactamente una S y una E?",
      answer: null,
      status: "PENDING",
      createdAt: isoAt(c1Start + 30 * MIN),
      answeredAt: null,
    },
  ]

  return {
    schemaVersion: MOCK_SCHEMA_VERSION,
    users,
    contests,
    contestProblems,
    problems,
    samples,
    teams,
    invitations,
    submissions,
    clarifications,
    refreshTokens: [],
    revokedSessions: [],
  }
}
