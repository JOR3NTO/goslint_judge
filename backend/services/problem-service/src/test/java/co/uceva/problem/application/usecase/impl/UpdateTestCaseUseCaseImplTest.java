package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.UpdateTestCaseUseCase.UpdateTestCaseCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTestCaseUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private UpdateTestCaseUseCaseImpl useCase;

    private final UUID testCaseId = UUID.randomUUID();

    @Test
    void shouldUpdateTestCaseWhenFound() {
        TestCase testCase = ProblemFixtures.aTestCase();
        testCase.setId(testCaseId);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTestCaseCommand command = new UpdateTestCaseCommand(
                testCaseId, "5", 2, false, "2 3", "5"
        );

        TestCase result = useCase.execute(command);

        assertThat(result.getExpectedOutput()).isEqualTo("5");
        assertThat(result.getOrderIndex()).isEqualTo(2);
        assertThat(result.isSample()).isFalse();
        verify(testCaseRepository).save(testCase);
    }

    @Test
    void shouldThrowWhenTestCaseNotFound() {
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.empty());
        UpdateTestCaseCommand command = new UpdateTestCaseCommand(
                testCaseId, "5", 2, false, "2 3", "5"
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(TestCaseNotFoundException.class);
    }
}
