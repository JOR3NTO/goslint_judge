package co.uceva.contest.infrastructure.web.mapper;

import java.util.UUID;

import co.uceva.contest.application.usecase.CreateContestUseCase.CreateContestCommand;
import co.uceva.contest.application.usecase.UpdateContestUseCase.UpdateContestCommand;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.infrastructure.web.dto.ContestResponseDTO;
import co.uceva.contest.infrastructure.web.dto.CreateContestRequestDTO;
import co.uceva.contest.infrastructure.web.dto.UpdateContestRequestDTO;

/**
 * Mapper encargado de convertir entre DTOs de la capa web y comandos/entidades
 * de la capa de aplicación y dominio para los concursos.
 */
public class ContestWebMapper {

    /**
     * Convierte un DTO de creación en un comando de aplicación.
     *
     * @param request DTO con los datos de creación.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static CreateContestCommand toCommand(CreateContestRequestDTO request) {
        return new CreateContestCommand(
                request.createdBy(),
                request.title(),
                request.description(),
                request.password(),
                request.startTime(),
                request.endTime()
        );
    }

    /**
     * Convierte un DTO de actualización y el identificador del concurso en un comando de aplicación.
     *
     * @param contestId Identificador del concurso a actualizar.
     * @param request   DTO con los datos de actualización.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static UpdateContestCommand toCommand(UUID contestId, UpdateContestRequestDTO request) {
        return new UpdateContestCommand(
                contestId,
                request.title(),
                request.description(),
                request.password(),
                request.startTime(),
                request.endTime(),
                request.status()
        );
    }

    /**
     * Convierte una entidad de dominio en un DTO de respuesta.
     *
     * @param domain Entidad {@link Contest}.
     * @return DTO con los datos expuestos al cliente.
     */
    public static ContestResponseDTO toResponse(Contest domain) {
        return new ContestResponseDTO(
                domain.getId(),
                domain.getCreatedBy(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }
}
