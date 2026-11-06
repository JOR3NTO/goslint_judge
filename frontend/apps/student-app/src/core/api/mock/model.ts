/**
 * Modelo de datos del modo mock: forma del estado en memoria (persistido en sessionStorage)
 * y constantes de la demo. Los DTO de respuesta son los mismos que usan las features.
 */
import type { ProblemDTO, InvitationDTO, TeamMemberDTO } from "@/features/contests/types/contest.types"
import type { SubmissionLanguage, SubmissionVerdict, SampleDTO } from "@/features/arena/types/arena.types"
import type { UserRole } from "@/features/profile/types/profile.types"
import { uid } from "./util"

// ─── Constantes de la demo ───────────────────────────────────────────────────

/** Clave versionada de sessionStorage. Si cambia el esquema, subir la versión. */
export const MOCK_STORAGE_KEY = "goslint:mock-db:v1"
export const MOCK_SCHEMA_VERSION = 1

/** Estudiante demo: credenciales documentadas en frontend/README.md. */
export const DEMO_STUDENT_EMAIL = "estudiante@uceva.edu.co"
export const DEMO_STUDENT_PASSWORD = "Demo1234!"
export const DEMO_STUDENT_ID = uid(1, 1)

/** Contraseña de las maratones con inscripción por contraseña (demo). */
export const DEMO_CONTEST_PASSWORD = "maraton123"

/** Todos los usuarios semilla comparten la contraseña demo. */
export const SEED_USER_PASSWORD = DEMO_STUDENT_PASSWORD

/** Firma falsa de los JWT del mock: un verificador real la rechazaría. */
export const MOCK_SIGNATURE = "mock-signature"

export const TEAM_MAX_MEMBERS = 3
export const ACCESS_TOKEN_TTL_MS = 15 * 60 * 1000
export const REFRESH_TOKEN_TTL_MS = 8 * 60 * 60 * 1000
/** Intentos fallidos antes de bloquear la cuenta (como AuthService del backend). */
export const LOGIN_MAX_FAILED_ATTEMPTS = 5
export const LOGIN_LOCK_MS = 15 * 60 * 1000

/** Penalización ICPC por intento fallido cuando la maratón no define otra. */
export const ICPC_WRONG_ATTEMPT_PENALTY_MIN = 20

// ─── Registros ───────────────────────────────────────────────────────────────

export interface UserRecord {
  id: string
  firstName: string
  lastName: string
  username: string
  email: string
  institution: string | null
  role: UserRole
  isActive: boolean
  createdAt: string
  passwordDigest: string
  failedLogins: number
  /** Epoch ms hasta el que la cuenta está bloqueada, o `null`. */
  lockedUntil: number | null
}

export interface ContestRecord {
  id: string
  createdBy: string
  title: string
  description: string
  startTime: string
  endTime: string
  createdAt: string
  passwordDigest: string
}

/** Problema dentro de una maratón (`ContestProblemDTO`). */
export interface ContestProblemRecord {
  id: string
  contestId: string
  problemId: string
  alias: string
  orderIndex: number
  points: number
  penalty: number
  balloonColor: string
}

export interface TeamRecord {
  id: string
  contestId: string
  captainId: string
  teamName: string
  createdAt: string
  members: TeamMemberDTO[]
}

export interface SubmissionRecord {
  id: string
  teamId: string
  problemId: string
  language: SubmissionLanguage
  sourceCode: string
  submittedAt: string
  /** Veredicto fijo (solo para envíos semilla); si es `null` se simula a partir del código. */
  verdictOverride: SubmissionVerdict | null
}

export interface ClarificationRecord {
  id: string
  /** Id de `ContestProblemRecord` (la ruta del backend usa el id de la relación). */
  contestProblemId: string
  teamId: string
  question: string
  answer: string | null
  status: "PENDING" | "ANSWERED"
  createdAt: string
  answeredAt: string | null
}

export interface RefreshRecord {
  token: string
  userId: string
  expiresAt: number
}

// ─── Estado completo ─────────────────────────────────────────────────────────

export interface MockState {
  schemaVersion: number
  users: UserRecord[]
  contests: ContestRecord[]
  contestProblems: ContestProblemRecord[]
  problems: ProblemDTO[]
  samples: SampleDTO[]
  teams: TeamRecord[]
  invitations: InvitationDTO[]
  submissions: SubmissionRecord[]
  clarifications: ClarificationRecord[]
  refreshTokens: RefreshRecord[]
  /** `jti` de los access tokens revocados por logout. */
  revokedSessions: string[]
}
