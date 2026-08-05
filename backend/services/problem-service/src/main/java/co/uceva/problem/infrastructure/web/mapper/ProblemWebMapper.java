package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateProblemUseCase.CreateProblemCommand;
import co.uceva.problem.application.usecase.UpdateProblemUseCase.UpdateProblemCommand;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.ProblemResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;

import java.util.UUID;

public class ProblemWebMapper {

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