package co.uceva.problem.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.CreateProblemUseCase;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;

@Service
public class CreateProblemUseCaseImpl implements CreateProblemUseCase{

    private final ProblemRepository problemRepository;

    public CreateProblemUseCaseImpl(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public Problem execute(CreateProblemCommand command) {
        Problem problem = Problem.create(
            command.createdBy(),
            command.title(),
            command.statement(),
            command.timeLimitMs(),
            command.memoryLimitKb(),
            command.difficulty(),
            command.inputFormat(),
            command.outputFormat()
        );

        return problemRepository.save(problem);
    }
}