package co.uceva.problem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

public interface UpdateTestCaseUseCase {
    TestCase execute(UpdateTestCaseCommand command);
    
    record UpdateTestCaseCommand(
        UUID testCaseId,
        String expectedOutput,
        int orderIndex,
        boolean isSample,
        String input,
        String output,
        Instant createdAt
    ) {}
}
