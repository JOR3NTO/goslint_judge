package co.uceva.problem.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetProblemByIdUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

@Service
public class GetProblemByIdUseCaseImpl implements GetProblemByIdUseCase{

    private final ProblemRepository problemRepository;

    public GetProblemByIdUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public Problem execute(UUID problemId){
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ProblemNotFoundException(problemId));
        return problem;
    }
}
