package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

public interface GetAllTestCaseByProblemIdUseCase {
    List<TestCase> execute(UUID problemId);
}
