package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteTestCaseUseCase;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class DeleteTestCaseUseCaseImpl implements DeleteTestCaseUseCase{
    
    private final TestCaseRepository testCaseRepository;

    public DeleteTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public void execute(UUID testCaseId){
        if (!testCaseRepository.findById(testCaseId).isPresent()) {
            throw new TestCaseNotFoundException(testCaseId);
        }
        testCaseRepository.deleteById(testCaseId);
    }
}
