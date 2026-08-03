package co.uceva.problem.application.usecase.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class CreateTestCaseUseCaseImpl implements CreateTestCaseUseCase {

    private final TestCaseRepository testCaseRepository;
    
    public CreateTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public TestCase execute(CreateTestCaseCommand command){
        TestCase testCase = TestCase.create(
            command.problemId(),
            command.expectedOutput(),
            command.orderIndex(),
            command.isSample(),
            command.input(),
            command.output()
        );
        return testCaseRepository.save(testCase);
    }
}
