package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteProblemUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.repository.ProblemRepository;

@Service
public class DeleteProblemUseCaseImpl implements DeleteProblemUseCase{
    
    private final ProblemRepository problemRepository;

    public DeleteProblemUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public void execute(UUID problemId){
        if (!problemRepository.findById(problemId).isPresent()) {
            throw new ProblemNotFoundException(problemId);
        }
        problemRepository.deleteById(problemId);
    }
}
