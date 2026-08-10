package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateTestCaseBatchUseCase;
import co.uceva.problem.application.usecase.CreateTestCaseUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class CreateTestCaseBatchUseCaseImpl implements CreateTestCaseBatchUseCase {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public CreateTestCaseBatchUseCaseImpl(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public List<TestCase> execute(CreateTestCaseBatchCommand command) {
        problemRepository.findById(command.problemId())
                .orElseThrow(() -> new ProblemNotFoundException(command.problemId()));

        testCaseRepository.deleteByProblemId(command.problemId());

        List<TestCase> newTestCases = command.testCases().stream()
                .map(tc -> TestCase.create(
                        command.problemId(),
                        tc.expectedOutput(),
                        tc.orderIndex(),
                        tc.isSample(),
                        tc.input(),
                        tc.output()
                ))
                .toList();

        return testCaseRepository.saveAll(newTestCases);
    }
}
