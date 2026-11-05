package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.fixtures.ProblemFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllProblemsByTitleUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private GetAllProblemsByTitleUseCaseImpl useCase;

    @Test
    void shouldReturnProblemsByTitle() {
        List<Problem> problems = List.of(
                ProblemFixtures.aProblem(UUID.randomUUID(), "Suma de enteros"),
                ProblemFixtures.aProblem(UUID.randomUUID(), "Suma de matrices")
        );
        when(problemRepository.findAllByTitle("Suma")).thenReturn(problems);

        List<Problem> result = useCase.execute("Suma");

        assertThat(result).isEqualTo(problems);
    }
}
