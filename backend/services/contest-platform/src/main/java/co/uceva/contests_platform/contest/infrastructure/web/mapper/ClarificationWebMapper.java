package co.uceva.contests_platform.contest.infrastructure.web.mapper;

import java.util.UUID;

import co.uceva.contests_platform.contest.application.usecase.AnswerClarificationUseCase.AnswerClarificationCommand;
import co.uceva.contests_platform.contest.application.usecase.AskClarificationUseCase.AskClarificationCommand;
import co.uceva.contests_platform.contest.domain.model.Clarification;
import co.uceva.contests_platform.contest.infrastructure.web.dto.AnswerClarificationRequestDTO;
import co.uceva.contests_platform.contest.infrastructure.web.dto.AskClarificationRequestDTO;
import co.uceva.contests_platform.contest.infrastructure.web.dto.ClarificationResponseDTO;

/**
 * Mapper entre DTOs de la capa web y comandos/entidades de aplicación y dominio para las aclaraciones.
 */
public class ClarificationWebMapper {

    public static AskClarificationCommand toCommand(UUID problemId, UUID userId, AskClarificationRequestDTO request) {
        return new AskClarificationCommand(problemId, userId, request.question());
    }

    public static AnswerClarificationCommand toCommand(UUID clarificationId, AnswerClarificationRequestDTO request) {
        return new AnswerClarificationCommand(clarificationId, request.answer());
    }

    public static ClarificationResponseDTO toResponse(Clarification domain) {
        return new ClarificationResponseDTO(
                domain.getId(),
                domain.getProblemId(),
                domain.getTeamId(),
                domain.getQuestion(),
                domain.getAnswer(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getAnsweredAt()
        );
    }
}
