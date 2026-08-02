package co.uceva.problem.application.usecase;

import java.util.List;

import co.uceva.problem.domain.model.TestCase;

public interface GetAllTestCaseUseCase {
    List<TestCase> execute();
}
