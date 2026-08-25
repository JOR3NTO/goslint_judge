package co.uceva.submission.infrastructure.web.mapper;

import co.uceva.submission.application.usecase.GetSubmissionMetricsUseCase.SubmissionMetrics;
import co.uceva.submission.application.usecase.SubmitCodeUseCase.SubmitCodeCommand;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.web.dto.SubmissionMetricsResponseDTO;
import co.uceva.submission.infrastructure.web.dto.SubmissionResponseDTO;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;

/**
 * Mapper encargado de convertir entre DTOs de la capa web y comandos/entidades
 * de la capa de aplicación y dominio para los envíos de código fuente.
 */
public class SubmissionWebMapper {

    /**
     * Convierte un DTO de creación de envío en un comando de aplicación.
     *
     * @param request DTO con los datos de creación.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static SubmitCodeCommand toCommand(SubmitCodeRequestDTO request) {
        return new SubmitCodeCommand(
                request.teamId(),
                request.problemId(),
                request.language(),
                request.sourceCode()
        );
    }

    /**
     * Convierte una entidad de dominio en un DTO de respuesta completo.
     *
     * @param domain Entidad {@link Submission}.
     * @return DTO con los datos expuestos al cliente.
     */
    public static SubmissionResponseDTO toResponse(Submission domain) {
        return new SubmissionResponseDTO(
                domain.getId(),
                domain.getTeamId(),
                domain.getProblemId(),
                domain.getLanguage(),
                domain.getSourceCode(),
                domain.getVerdict(),
                domain.getExecutionTimeMs(),
                domain.getMemoryUsedKb(),
                domain.getCodeSizeBytes(),
                domain.getSubmittedAt()
        );
    }

    /**
     * Convierte las métricas de evaluación en un DTO de respuesta.
     *
     * @param metrics Métricas obtenidas del caso de uso.
     * @return DTO con las métricas expuestas al cliente.
     */
    public static SubmissionMetricsResponseDTO toResponse(SubmissionMetrics metrics) {
        return new SubmissionMetricsResponseDTO(
                metrics.submissionId(),
                metrics.verdict(),
                metrics.executionTimeMs(),
                metrics.memoryUsedKb(),
                metrics.codeSizeBytes()
        );
    }
}
