package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetTestCaseByIdUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class GetTestCaseByIdUseCaseImpl implements GetTestCaseByIdUseCase{
    private final TestCaseRepository testCaseRepository;

    public GetTestCaseByIdUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public TestCase execute(UUID TestCaseId){
        TestCase testCase = testCaseRepository.findById(TestCaseId).
        orElseThrow(() -> new TestCaseNotFoundException(TestCaseId));
        return testCase;
    }
}
