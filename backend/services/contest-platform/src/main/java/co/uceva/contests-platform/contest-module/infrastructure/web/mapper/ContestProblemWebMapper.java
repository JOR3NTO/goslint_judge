package co.uceva.contestsplatform.contestmodule.infrastructure.web.mapper;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.application.usecase.AddProblemToContestUseCase.AddProblemToContestCommand;
import co.uceva.contestsplatform.contestmodule.application.usecase.UpdateContestProblemUseCase.UpdateContestProblemCommand;
import co.uceva.contestsplatform.contestmodule.domain.model.ContestProblem;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.AddContestProblemRequestDTO;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.ContestProblemResponseDTO;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.UpdateContestProblemRequestDTO;

/**
 * Mapper entre DTOs de la capa web y comandos/entidades de aplicación y dominio
 * para los problemas de un concurso.
 */
public class ContestProblemWebMapper {

    public static AddProblemToContestCommand toCommand(UUID contestId, AddContestProblemRequestDTO request) {
        int points = request.points() != null ? request.points() : ContestProblem.DEFAULT_POINTS;
        int penalty = request.penalty() != null
                ? request.penalty() : ContestProblem.DEFAULT_PENALTY;
        return new AddProblemToContestCommand(contestId, request.problemId(), request.alias(),
                request.orderIndex(), points, penalty, request.balloonColor());
    }

    public static UpdateContestProblemCommand toCommand(UUID contestProblemId, UpdateContestProblemRequestDTO request) {
        return new UpdateContestProblemCommand(contestProblemId, request.alias(), request.orderIndex(),
                request.points(), request.penalty(), request.balloonColor());
    }

    public static ContestProblemResponseDTO toResponse(ContestProblem domain) {
        return new ContestProblemResponseDTO(
                domain.getId(),
                domain.getContestId(),
                domain.getProblemId(),
                domain.getAlias(),
                domain.getOrderIndex(),
                domain.getPoints(),
                domain.getPenalty(),
                domain.getBalloonColor().value()
        );
    }
}
