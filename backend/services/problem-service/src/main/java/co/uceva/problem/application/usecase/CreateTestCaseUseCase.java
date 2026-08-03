package co.uceva.problem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

public interface CreateTestCaseUseCase {
    TestCase execute(CreateTestCaseCommand command);

    record CreateTestCaseCommand(
        UUID problemId,
        String expectedOutput,
        int orderIndex,
        boolean isSample,
        String input,
        String output
    ) {}
}
