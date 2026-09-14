package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
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
class CreateTestCaseUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private CreateTestCaseUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldCreateAndSaveTestCase() {
        CreateTestCaseCommand command = new CreateTestCaseCommand(
                problemId, "3", 1, true, "1 2", "3"
        );
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestCase result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getProblemId()).isEqualTo(problemId);
        assertThat(result.getOrderIndex()).isEqualTo(1);
        assertThat(result.isSample()).isTrue();
        verify(testCaseRepository).save(any(TestCase.class));
    }
}
