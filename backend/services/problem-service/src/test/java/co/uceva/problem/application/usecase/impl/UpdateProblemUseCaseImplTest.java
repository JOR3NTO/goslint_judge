package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.UpdateProblemUseCase.UpdateProblemCommand;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.fixtures.ProblemFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProblemUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private UpdateProblemUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldUpdateProblemWhenFound() {
        Problem problem = ProblemFixtures.aProblem(problemId, "Original");
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProblemCommand command = new UpdateProblemCommand(
                problemId, "Updated", "Updated statement", 2000, 131072, 1200, "updated input", "updated output"
        );

        Problem result = useCase.execute(command);

        assertThat(result.getTitle()).isEqualTo("Updated");
        assertThat(result.getTimeLimitMs()).isEqualTo(2000);
        assertThat(result.getMemoryLimitKb()).isEqualTo(131072);
        assertThat(result.getDifficult()).isEqualTo(1200);
        verify(problemRepository).save(problem);
    }

    @Test
    void shouldThrowWhenProblemNotFound() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());
        UpdateProblemCommand command = new UpdateProblemCommand(
                problemId, "Updated", "Statement", 1000, 65536, 800, "input", "output"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ProblemNotFoundException.class);
    }
}
