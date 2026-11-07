package co.uceva.contestsplatform.contestmodule.infrastructure.web.mapper;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.application.usecase.AnswerClarificationUseCase.AnswerClarificationCommand;
import co.uceva.contestsplatform.contestmodule.application.usecase.AskClarificationUseCase.AskClarificationCommand;
import co.uceva.contestsplatform.contestmodule.domain.model.Clarification;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.AnswerClarificationRequestDTO;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.AskClarificationRequestDTO;
import co.uceva.contestsplatform.contestmodule.infrastructure.web.dto.ClarificationResponseDTO;

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
