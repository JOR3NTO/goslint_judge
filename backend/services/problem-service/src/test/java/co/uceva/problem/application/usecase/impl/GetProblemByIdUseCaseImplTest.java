package co.uceva.problem.application.usecase.impl;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProblemByIdUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private GetProblemByIdUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldReturnProblemWhenFound() {
        Problem problem = ProblemFixtures.aProblem(problemId, "Title");
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

        Problem result = useCase.execute(problemId);

        assertThat(result).isEqualTo(problem);
    }

    @Test
    void shouldThrowWhenProblemNotFound() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(problemId))
                .isInstanceOf(ProblemNotFoundException.class);
    }
}
