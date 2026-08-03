package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

public interface UpdateProblemUseCase {
    Problem execute(UpdateProblemCommand command);
    
    record UpdateProblemCommand(
        UUID problemId,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficultyRating,
        String inputFormat,
        String outputFormat
    ){}
}
