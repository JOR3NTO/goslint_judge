package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;

public interface CreateTestCaseBatchUseCase {
    List<TestCase> execute(CreateTestCaseBatchCommand command);

    record CreateTestCaseBatchCommand(
        UUID problemId,
        List<CreateTestCaseCommand> testCases
    ) {}
}
