package co.uceva.problem.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.UpdateTestCaseUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class UpdateTestCaseUseCaseImpl implements UpdateTestCaseUseCase {
    
    private final TestCaseRepository testCaseRepository;

    public UpdateTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public TestCase execute(UpdateTestCaseCommand command){
        TestCase testCase = testCaseRepository.findById(command.testCaseId())
        .orElseThrow(() -> new TestCaseNotFoundException(command.testCaseId()));

        testCase.update(
            command.expectedOutput(),
            command.orderIndex(),
            command.isSample(),
            command.input(),
            command.output());
        return testCaseRepository.save(testCase);
    }
}
