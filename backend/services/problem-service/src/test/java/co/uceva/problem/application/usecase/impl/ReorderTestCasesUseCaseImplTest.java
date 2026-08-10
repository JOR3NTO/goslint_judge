package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderTestCasesUseCaseImplTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private ReorderTestCasesUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<Map<UUID, Integer>> orderCaptor;

    private final UUID problemId = UUID.randomUUID();

    @Test
    void shouldUpdateOrderIndexesToOneBasedSequence() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<TestCase> current = List.of(
                TestCase.builder().id(first).problemId(problemId).expectedOutput("a").orderIndex(3).isSample(true).input("in").output("out").createdAt(Instant.now()).build(),
                TestCase.builder().id(second).problemId(problemId).expectedOutput("b").orderIndex(1).isSample(false).input("in").output("out").createdAt(Instant.now()).build(),
                TestCase.builder().id(third).problemId(problemId).expectedOutput("c").orderIndex(2).isSample(false).input("in").output("out").createdAt(Instant.now()).build()
        );
        List<UUID> ordered = List.of(second, third, first);

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(new Problem()));
        when(testCaseRepository.findAllByProblemId(problemId)).thenReturn(current);

        useCase.execute(problemId, ordered);

        verify(testCaseRepository).updateOrderIndexes(orderCaptor.capture());
        Map<UUID, Integer> captured = orderCaptor.getValue();
        assertThat(captured).containsEntry(second, 1);
        assertThat(captured).containsEntry(third, 2);
        assertThat(captured).containsEntry(first, 3);
    }

    @Test
    void shouldThrowWhenProblemNotFound() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(problemId, List.of(UUID.randomUUID())))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void shouldThrowWhenOrderSizeDoesNotMatchCurrentTestCases() {
        when(problemRepository.findById(problemId)).thenReturn(Optional.of(new Problem()));
        when(testCaseRepository.findAllByProblemId(problemId)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(problemId, List.of(UUID.randomUUID())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cantidad de IDs");
    }
}
