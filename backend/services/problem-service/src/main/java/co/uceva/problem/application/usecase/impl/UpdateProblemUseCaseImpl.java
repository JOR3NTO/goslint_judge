package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.UpdateProblemUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProblemUseCaseImpl implements UpdateProblemUseCase {

    private final ProblemRepository problemRepository;

    public UpdateProblemUseCaseImpl(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public Problem execute(UpdateProblemCommand command) {
        Problem problem = problemRepository.findById(command.problemId())
                .orElseThrow(() -> new ProblemNotFoundException(command.problemId()));

        TimeLimit newTimeLimit = new TimeLimit(command.timeLimitMs());
        MemoryLimit newMemoryLimit = new MemoryLimit(command.memoryLimitKb());
        Difficulty newDifficulty = new Difficulty(command.difficultyRating());

        problem.update(
                command.title(),
                command.statement(),
                newTimeLimit,
                newMemoryLimit,
                newDifficulty,
                command.inputFormat(),
                command.outputFormat()
        );

        return problemRepository.save(problem);
    }
}