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

/**
 * Pruebas unitarias para {@link GetAllSampleTestCasesByProblemIdUseCaseImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GetAllSampleTestCasesByProblemIdUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private GetAllSampleTestCasesByProblemIdUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    /**
     * Verifica que el caso de uso retorne únicamente los casos de prueba
     * de ejemplo asociados al problema.
     */
    @Test
    void shouldReturnOnlySampleTestCasesByProblemId() {
        TestCase sampleTestCase = ProblemFixtures.aTestCase(problemId, 1);
        when(testCaseRepository.findAllSampleByProblemId(problemId)).thenReturn(List.of(sampleTestCase));

        List<TestCase> result = useCase.execute(problemId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isSample()).isTrue();
    }

    /**
     * Verifica que el caso de uso retorne una lista vacía cuando el problema
     * no tiene casos de prueba de ejemplo.
     */
    @Test
    void shouldReturnEmptyListWhenNoSampleTestCasesExist() {
        when(testCaseRepository.findAllSampleByProblemId(problemId)).thenReturn(List.of());

        List<TestCase> result = useCase.execute(problemId);

        assertThat(result).isEmpty();
    }
}
