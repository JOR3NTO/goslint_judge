package co.uceva.problem.application.usecase.impl;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.problem.application.usecase.DeleteTestCaseBatchUseCase;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;

@Service
public class DeleteTestCaseBatchUseCaseImpl implements DeleteTestCaseBatchUseCase {

    private final TestCaseRepository testCaseRepository;

    public DeleteTestCaseBatchUseCaseImpl(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    @Transactional
    public void execute(List<UUID> testCaseIds) {
        if (testCaseIds == null || testCaseIds.isEmpty()) {
            return;
        }

        Set<UUID> problemIdsAffected = new HashSet<>();
        List<UUID> existingIds = new ArrayList<>();

        for (UUID testCaseId : testCaseIds) {
            Optional<TestCase> testCase = testCaseRepository.findById(testCaseId);
            if (testCase.isPresent()) {
                existingIds.add(testCaseId);
                problemIdsAffected.add(testCase.get().getProblemId());
            }
        }

        if (!existingIds.isEmpty()) {
            testCaseRepository.deleteAllById(existingIds);
        }

        for (UUID problemId : problemIdsAffected) {
            List<TestCase> remaining = testCaseRepository.findAllByProblemId(problemId);
            if (remaining.isEmpty()) {
                continue;
            }

            Map<UUID, Integer> newOrders = new LinkedHashMap<>();
            for (int i = 0; i < remaining.size(); i++) {
                newOrders.put(remaining.get(i).getId(), i + 1);
            }
            testCaseRepository.updateOrderIndexes(newOrders);
        }
    }
}
