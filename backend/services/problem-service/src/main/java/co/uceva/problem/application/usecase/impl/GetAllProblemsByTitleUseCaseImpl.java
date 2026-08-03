package co.uceva.problem.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllProblemsByTitleUseCase;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

@Service
public class GetAllProblemsByTitleUseCaseImpl implements GetAllProblemsByTitleUseCase{
    
    private final ProblemRepository problemRepository;

    public GetAllProblemsByTitleUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public List<Problem> execute(String title){
        return problemRepository.findAllByTitle(title);
    }
}
