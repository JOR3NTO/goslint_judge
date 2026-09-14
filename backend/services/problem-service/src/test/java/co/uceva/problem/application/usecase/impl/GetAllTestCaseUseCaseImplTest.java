package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
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
class GetAllTestCaseUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private GetAllTestCaseUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldReturnAllTestCasesByProblemId() {
        List<TestCase> testCases = List.of(
                ProblemFixtures.aTestCase(problemId, 1),
                ProblemFixtures.aTestCase(problemId, 2)
        );
        when(testCaseRepository.findAllByProblemId(problemId)).thenReturn(testCases);

        List<TestCase> result = useCase.execute(problemId);

        assertThat(result).hasSize(2);
    }
}
