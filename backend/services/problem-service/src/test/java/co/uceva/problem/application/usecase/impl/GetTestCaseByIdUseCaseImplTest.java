package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.domain.exception.TestCaseNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
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
class GetTestCaseByIdUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private GetTestCaseByIdUseCaseImpl useCase;

    private final UUID testCaseId = UUID.randomUUID();

    @Test
    void shouldReturnTestCaseWhenFound() {
        TestCase testCase = ProblemFixtures.aTestCase();
        testCase.setId(testCaseId);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));

        TestCase result = useCase.execute(testCaseId);

        assertThat(result).isEqualTo(testCase);
    }

    @Test
    void shouldThrowWhenTestCaseNotFound() {
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(testCaseId))
                .isInstanceOf(TestCaseNotFoundException.class);
    }
}
