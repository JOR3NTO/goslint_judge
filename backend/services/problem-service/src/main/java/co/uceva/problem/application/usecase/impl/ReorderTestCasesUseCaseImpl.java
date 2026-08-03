package co.uceva.problem.application.usecase.impl;

import co.uceva.problem.application.usecase.ReorderTestCasesUseCase;
import co.uceva.problem.domain.exception.ProblemNotFoundException;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.domain.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReorderTestCasesUseCaseImpl implements ReorderTestCasesUseCase {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    public ReorderTestCasesUseCaseImpl(ProblemRepository problemRepository, TestCaseRepository testCaseRepository) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public void execute(UUID problemId, List<UUID> orderedTestCaseIds) {
        problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        List<TestCase> currentTestCases = testCaseRepository.findAllByProblemId(problemId);
        if (currentTestCases.size() != orderedTestCaseIds.size()) {
            throw new RuntimeException(
                    "La cantidad de IDs enviados no coincide con el total de casos de prueba del problema."
            );
        }
        Map<UUID, Integer> newOrdersMap = new HashMap<>();
        for (int i = 0; i < orderedTestCaseIds.size(); i++) {
            UUID testCaseId = orderedTestCaseIds.get(i);
            int newOrderIndex = i + 1; // Secuencia base 1 (1, 2, 3...)
            newOrdersMap.put(testCaseId, newOrderIndex);
        }

        testCaseRepository.updateOrderIndexes(newOrdersMap);
    }
}