package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateProblemUseCase.CreateProblemCommand;
import co.uceva.problem.application.usecase.UpdateProblemUseCase.UpdateProblemCommand;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.ProblemResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;

import java.util.UUID;

/**
 * Mapper encargado de convertir entre DTOs de la capa web y comandos/entidades
 * de la capa de aplicación y dominio para los problemas.
 */
public class ProblemWebMapper {

    /**
     * Convierte un DTO de creación en un comando de aplicación.
     *
     * @param request DTO con los datos de creación.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static CreateProblemCommand toCommand(CreateProblemRequestDTO request) {
        return new CreateProblemCommand(
                request.createdBy(),
                request.title(),
                request.statement(),
                request.timeLimitMs(),
                request.memoryLimitKb(),
                request.difficult(),
                request.inputFormat(),
                request.outputFormat()
        );
    }

    /**
     * Convierte un DTO de actualización y el identificador del problema en un comando de aplicación.
     *
     * @param problemId Identificador del problema a actualizar.
     * @param request   DTO con los datos de actualización.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static UpdateProblemCommand toCommand(UUID problemId, UpdateProblemRequestDTO request) {
        return new UpdateProblemCommand(
                problemId,
                request.title(),
                request.statement(),
                request.timeLimitMs(),
                request.memoryLimitKb(),
                request.difficult(),
                request.inputFormat(),
                request.outputFormat()
        );
    }

    /**
     * Convierte una entidad de dominio en un DTO de respuesta.
     *
     * @param domain Entidad {@link Problem}.
     * @return DTO con los datos expuestos al cliente.
     */
    public static ProblemResponseDTO toResponse(Problem domain) {
        return new ProblemResponseDTO(
                domain.getId(),
                domain.getCreatedBy(),
                domain.getTitle(),
                domain.getStatement(),
                domain.getTimeLimitMs(),
                domain.getMemoryLimitKb(),
                domain.getDifficult(),
                domain.getCreatedAt(),
                domain.getInputFormat(),
                domain.getOutputFormat()
        );
    }
}