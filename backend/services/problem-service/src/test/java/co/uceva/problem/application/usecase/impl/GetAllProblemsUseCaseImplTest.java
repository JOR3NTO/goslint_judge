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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllProblemsUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private GetAllProblemsUseCaseImpl useCase;

    @Test
    void shouldReturnAllProblems() {
        List<Problem> problems = List.of(
                ProblemFixtures.aProblem(),
                ProblemFixtures.aProblem()
        );
        when(problemRepository.findAll()).thenReturn(problems);

        List<Problem> result = useCase.execute();

        assertThat(result).hasSize(2);
    }
}
