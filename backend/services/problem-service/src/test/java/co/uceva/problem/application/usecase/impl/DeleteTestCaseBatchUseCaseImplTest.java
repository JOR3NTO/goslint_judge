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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTestCaseBatchUseCaseImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private DeleteTestCaseBatchUseCaseImpl useCase;

    @Test
    void shouldDeleteExistingAndRecalculateOrderIndex() {
        UUID problemId = UUID.randomUUID();
        UUID tc1 = UUID.randomUUID();
        UUID tc2 = UUID.randomUUID();
        UUID tc3 = UUID.randomUUID();

        TestCase case1 = ProblemFixtures.aTestCase(problemId, 1);
        case1.setId(tc1);
        TestCase case2 = ProblemFixtures.aTestCase(problemId, 2);
        case2.setId(tc2);
        TestCase case3 = ProblemFixtures.aTestCase(problemId, 3);
        case3.setId(tc3);

        when(testCaseRepository.findById(tc1)).thenReturn(Optional.of(case1));
        when(testCaseRepository.findById(tc2)).thenReturn(Optional.of(case2));
        when(testCaseRepository.findById(tc3)).thenReturn(Optional.empty());

        when(testCaseRepository.findAllByProblemId(problemId)).thenReturn(List.of(case3));

        useCase.execute(List.of(tc1, tc2, tc3));

        verify(testCaseRepository).deleteAllById(List.of(tc1, tc2));
        verify(testCaseRepository).updateOrderIndexes(argThat(map ->
                map.size() == 1 && map.get(tc3) == 1
        ));
    }

    @Test
    void shouldDoNothingWhenAllIdsAreNonExisting() {
        UUID tc1 = UUID.randomUUID();
        when(testCaseRepository.findById(tc1)).thenReturn(Optional.empty());

        useCase.execute(List.of(tc1));

        verifyNoMoreInteractions(testCaseRepository);
    }

    @Test
    void shouldDoNothingForEmptyList() {
        useCase.execute(List.of());
        verifyNoMoreInteractions(testCaseRepository);
    }
}
