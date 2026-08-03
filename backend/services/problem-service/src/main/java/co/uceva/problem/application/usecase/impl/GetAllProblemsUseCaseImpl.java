package co.uceva.problem.application.usecase.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.GetAllProblemsUseCase;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

@Service
public class GetAllProblemsUseCaseImpl implements GetAllProblemsUseCase{
    
    private final ProblemRepository problemRepository;

    public GetAllProblemsUseCaseImpl(ProblemRepository problemRepository){
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public List<Problem> execute(){
        return problemRepository.findAll();
    }
}
