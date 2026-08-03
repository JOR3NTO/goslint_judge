package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllTestCaseByProblemIdUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class GetAllTestCaseUseCaseImpl implements GetAllTestCaseByProblemIdUseCase{

    private final TestCaseRepository testCaseRepository;

    public GetAllTestCaseUseCaseImpl(TestCaseRepository testCaseRepository){
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public List<TestCase> execute(UUID problemId){
        return testCaseRepository.findAllByProblemId(problemId);
    }
}
