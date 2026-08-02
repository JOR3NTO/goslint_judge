package co.uceva.problem.application.usecase;

import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

public interface GetTestCaseByIdUseCase {
    TestCase execute(UUID testCaseId);
}
