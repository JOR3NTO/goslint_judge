/**
 * PublicContestsService — obtiene maratones visibles públicamente.
 *
 * Estado actual: usa mock data estático.
 *
 * TODO (backend pendiente): conectar a:
 *   GET /api/public/contests?status=active|upcoming|past
 *   - No requiere autenticación (endpoint público).
 *   - Respuesta esperada: { contests: PublicContest[] }
 *   - Soportar Cache-Control: public, max-age=60 en el backend.
 *
 * Para activar el fetch real, descomentar el bloque marcado con "REAL API"
 * y eliminar el return de mock data debajo.
 */
import type { PublicContest, ContestStatus } from "../types/public-contest.types"

// ─── Mock data ────────────────────────────────────────────────────────────────

const MOCK_CONTESTS: PublicContest[] = [
  // Activas
  {
    id: "active-1",
    title: "Maratón Nacional de Algoritmos 2025",
    description:
      "La competencia más grande del año. Premios especiales para los top 10. Problemas de grafos, DP y geometría computacional.",
    status: "active",
    startDate: "26 Sep 2025",
    endDate: "26 Sep 2025",
    duration: "5 horas",
    participantCount: 1205,
    problemCount: 10,
    difficulty: "Avanzado",
  },
  {
    id: "active-2",
    title: "Goslint Weekly Challenge #42",
    description:
      "Competencia semanal con problemas de estructuras de datos y algoritmos de grafos. Ideal para preparar ICPC.",
    status: "active",
    startDate: "26 Sep 2025",
    endDate: "26 Sep 2025",
    duration: "3 horas",
    participantCount: 234,
    problemCount: 6,
    difficulty: "Intermedio",
  },
  // Próximas
  {
    id: "upcoming-1",
    title: "ICPC South America Qualifier — Simulacro",
    description:
      "Simulacro oficial del clasificatorio ICPC. Mismas reglas, mismo formato, problemas de nivel regional.",
    status: "upcoming",
    startDate: "5 Oct 2025",
    endDate: "5 Oct 2025",
    duration: "5 horas",
    participantCount: 0,
    problemCount: 11,
    difficulty: "Avanzado",
  },
  {
    id: "upcoming-2",
    title: "Práctica: Programación Dinámica",
    description:
      "Sesión de práctica enfocada en problemas clásicos de DP: knapsack, LCS, digit DP y más.",
    status: "upcoming",
    startDate: "3 Oct 2025",
    endDate: "3 Oct 2025",
    duration: "2 horas",
    participantCount: 0,
    problemCount: 5,
    difficulty: "Principiante",
  },
  {
    id: "upcoming-3",
    title: "Goslint Weekly Challenge #43",
    description:
      "Próxima entrega de la serie semanal. Problemas de árboles, BFS/DFS y estructuras de datos avanzadas.",
    status: "upcoming",
    startDate: "3 Oct 2025",
    endDate: "3 Oct 2025",
    duration: "3 horas",
    participantCount: 0,
    problemCount: 6,
    difficulty: "Intermedio",
  },
  // Pasadas
  {
    id: "past-1",
    title: "Goslint Weekly Challenge #41",
    description:
      "Competencia semanal completada. 312 participantes. Problemas de strings, hashing y two pointers.",
    status: "past",
    startDate: "19 Sep 2025",
    endDate: "19 Sep 2025",
    duration: "3 horas",
    participantCount: 312,
    problemCount: 6,
    difficulty: "Intermedio",
  },
  {
    id: "past-2",
    title: "Práctica: Grafos y BFS/DFS",
    description:
      "Sesión de práctica completada. Cubrió componentes conexas, caminos mínimos y detección de ciclos.",
    status: "past",
    startDate: "15 Sep 2025",
    endDate: "15 Sep 2025",
    duration: "2 horas",
    participantCount: 89,
    problemCount: 5,
    difficulty: "Principiante",
  },
  {
    id: "past-3",
    title: "Clasificatorio Regional UCEVA 2025",
    description:
      "Clasificatorio interno UCEVA. Top 3 clasificaron para la fase regional. Nivel universitario avanzado.",
    status: "past",
    startDate: "8 Sep 2025",
    endDate: "8 Sep 2025",
    duration: "4 horas",
    participantCount: 87,
    problemCount: 8,
    difficulty: "Avanzado",
  },
]

// ─── Servicio ─────────────────────────────────────────────────────────────────

export async function getPublicContests(
  status: ContestStatus
): Promise<PublicContest[]> {
  // ── REAL API (descomentar cuando el backend implemente el endpoint) ──────────
  // try {
  //   const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
  //   const res = await fetch(`${apiUrl}/api/public/contests?status=${status}`, {
  //     next: { revalidate: 60 },  // revalidar cada 60s si se usa ISR
  //   })
  //   if (!res.ok) throw new Error(`HTTP ${res.status}`)
  //   const { contests } = await res.json()
  //   return contests as PublicContest[]
  // } catch {
  //   // Fallback a mock si el backend no responde
  //   return MOCK_CONTESTS.filter((c) => c.status === status)
  // }
  // ────────────────────────────────────────────────────────────────────────────

  // Mock mientras no existe el endpoint:
  return MOCK_CONTESTS.filter((c) => c.status === status)
}

export async function getAllPublicContests(): Promise<PublicContest[]> {
  return MOCK_CONTESTS
}
