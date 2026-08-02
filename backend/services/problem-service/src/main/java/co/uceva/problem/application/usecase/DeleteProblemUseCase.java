package co.uceva.problem.application.usecase;

import java.util.UUID;

public interface DeleteProblemUseCase {
    void execute(UUID problemId);
}
