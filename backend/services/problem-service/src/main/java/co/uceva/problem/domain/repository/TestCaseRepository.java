package co.uceva.problem.domain.repository;

import co.uceva.problem.domain.model.TestCase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TestCaseRepository {
    TestCase save(TestCase testCase);
    List<TestCase> saveAll(List<TestCase> testCases);
    void deleteById(UUID testCaseId);
    void deleteAllById(List<UUID> testCaseIds);
    void deleteByProblemId(UUID problemId);
    Optional<TestCase> findById(UUID testCaseId);
    List<TestCase> findAllByProblemId(UUID problemId);
    void updateOrderIndexes(Map<UUID, Integer> newOrders);
}