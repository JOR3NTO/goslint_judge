package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

public interface CreateProblemUseCase {
    Problem execute(CreateProblemCommand command);

    record CreateProblemCommand(
        UUID createdBy,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficulty,
        String inputFormat,
        String outputFormat
    ){}
}