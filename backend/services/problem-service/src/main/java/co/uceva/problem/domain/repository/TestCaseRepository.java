package co.uceva.problem.domain.repository;

import co.uceva.problem.domain.model.TestCase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TestCaseRepository {
    TestCase save(TestCase testCase);
    void deleteById(UUID testCaseId);
    Optional<TestCase> findById(UUID testCaseId);
    List<TestCase> findAllByProblemId(UUID problemId);
    void updateOrderIndexes(Map<UUID, Integer> newOrders);
}