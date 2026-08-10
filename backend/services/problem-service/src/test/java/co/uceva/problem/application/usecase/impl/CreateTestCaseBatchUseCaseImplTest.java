package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.CreateTestCaseBatchUseCase.CreateTestCaseBatchCommand;
import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;
import co.uceva.problem.fixtures.ProblemFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTestCaseBatchUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private CreateTestCaseBatchUseCaseImpl useCase;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldReplaceAllTestCasesWhenProblemExists() {
        Problem problem = ProblemFixtures.aProblem(problemId, "Test");
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

        List<CreateTestCaseCommand> commands = List.of(
                new CreateTestCaseCommand(problemId, "3", 1, true, "1 2", "3"),
                new CreateTestCaseCommand(problemId, "7", 2, false, "3 4", "7")
        );
        CreateTestCaseBatchCommand command = new CreateTestCaseBatchCommand(problemId, commands);

        when(testCaseRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TestCase> testCases = invocation.getArgument(0);
            testCases.forEach(tc -> tc.setId(UUID.randomUUID()));
            return testCases;
        });

        List<TestCase> result = useCase.execute(command);

        assertThat(result).hasSize(2);
        verify(problemRepository).findById(problemId);
        verify(testCaseRepository).deleteByProblemId(problemId);
        verify(testCaseRepository).saveAll(any());
    }

    @Test
    void shouldThrowWhenProblemNotFound() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        CreateTestCaseBatchCommand command = new CreateTestCaseBatchCommand(problemId, List.of());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ProblemNotFoundException.class);
    }
}
