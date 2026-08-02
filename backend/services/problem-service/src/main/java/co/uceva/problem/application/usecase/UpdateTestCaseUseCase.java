package co.uceva.problem.application.usecase;

import java.time.Instant;

import co.uceva.problem.domain.model.TestCase;

public interface UpdateTestCaseUseCase {
    TestCase execute(UpdateTestCaseCommand testCase);
    
    record UpdateTestCaseCommand(
        String expectedOutput,
        int orderIndex,
        boolean isSample,
        String input,
        String output,
        Instant createdAt
    ) {}
}
