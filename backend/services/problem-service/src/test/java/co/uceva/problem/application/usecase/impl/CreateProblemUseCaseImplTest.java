package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.CreateProblemUseCase.CreateProblemCommand;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProblemUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private CreateProblemUseCaseImpl useCase;

    @Test
    void shouldCreateAndSaveProblem() {
        CreateProblemCommand command = new CreateProblemCommand(
                UUID.randomUUID(), "Suma", "Statement", 1000, 65536, 800, "input", "output"
        );
        when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Problem result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Suma");
        assertThat(result.getTimeLimitMs()).isEqualTo(1000);
        verify(problemRepository).save(any(Problem.class));
    }
}
