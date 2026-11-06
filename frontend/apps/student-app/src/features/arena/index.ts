// Public API of the arena feature (vista de competencia de una maratón)
export { ContestArenaPage } from "./components/arena-page"

// Tipos del contrato con el submission-service y modelos de vista
export type {
  ArenaProblem,
  SampleDTO,
  SubmissionDTO,
  SubmissionLanguage,
  SubmissionStatus,
  SubmissionVerdict,
  SubmitSolutionRequest,
  TeamProblemStatus,
} from "./types/arena.types"

// Servicio y errores tipados
export { ArenaService, ArenaApiError, createArenaService } from "./services/arena-service"
