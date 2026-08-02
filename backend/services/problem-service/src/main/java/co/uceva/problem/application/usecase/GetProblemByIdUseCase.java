package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.Problem;

public interface GetProblemByIdUseCase {
    Problem execute(UUID problemId);
}
