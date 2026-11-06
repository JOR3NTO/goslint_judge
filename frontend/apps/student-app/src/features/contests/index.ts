// Public API of the contests feature
export { ContestsPage } from "./components/contests-page"
export { ContestDetailPage } from "./components/contest-detail-page"
export { PastContestsPage } from "./components/past-contests-page"
export { ContestCard } from "./components/contest-card"

// Tipos del contrato con el backend y modelos de vista
export type {
  ContestDTO,
  ContestStatus,
  ContestProblemDTO,
  TeamDTO,
  TeamMemberDTO,
  ProblemDTO,
  ContestListItem,
} from "./types/contest.types"

// Servicio y errores tipados (para otras features que necesiten consultar maratones)
export { ContestsService, ContestApiError } from "./services/contests-service"
export { createContestsService } from "./services/contests-api"
