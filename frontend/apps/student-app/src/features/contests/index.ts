// Public API of the contests feature
export { ContestsPage } from "./components/contests-page"
export { ContestDetailPage } from "./components/contest-detail-page"
export { PastContestsPage } from "./components/past-contests-page"
export { ContestCard } from "./components/contest-card"
export { ContestTimer } from "./components/contest-timer"
export { InvitationsPage } from "./components/invitations-page"

// Contador de invitaciones PENDING para la navbar (best effort, null si no hay sesión o falla)
export { usePendingInvitationsCount } from "./hooks/use-pending-invitations-count"

// Sesión leída de localStorage (token + claim sub). Lo usan otras features que llaman al backend.
export { readContestSession } from "./utils/session"
export type { ContestSession } from "./utils/session"

// Tipos del contrato con el backend y modelos de vista
export type {
  ContestDTO,
  ContestStatus,
  ContestProblemDTO,
  TeamDTO,
  TeamMemberDTO,
  ProblemDTO,
  ContestListItem,
  InvitationDTO,
  InvitationStatus,
} from "./types/contest.types"

// Servicio y errores tipados (para otras features que necesiten consultar maratones)
export { ContestsService, ContestApiError } from "./services/contests-service"
export { createContestsService } from "./services/contests-api"
