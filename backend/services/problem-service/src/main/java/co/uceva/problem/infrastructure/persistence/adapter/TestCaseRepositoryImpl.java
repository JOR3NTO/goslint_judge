package co.uceva.problem.infrastructure.persistence.adapter;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
import co.uceva.problem.infrastructure.mapper.TestCaseEntityMapper;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;
import co.uceva.problem.infrastructure.persistence.repository.SpringDataTestCaseRepository;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TestCaseRepositoryImpl implements TestCaseRepository {

    private final SpringDataTestCaseRepository springDataRepository;

    public TestCaseRepositoryImpl(SpringDataTestCaseRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public TestCase save(TestCase testCase) {
        TestCaseEntity entity = TestCaseEntityMapper.toEntity(testCase);
        TestCaseEntity saved = springDataRepository.save(entity);
        return TestCaseEntityMapper.toDomain(saved);
    }

    public List<TestCase> saveAll(List<TestCase> testCases) {
        List<TestCaseEntity> entities = testCases.stream()
                .map(TestCaseEntityMapper::toEntity)
                .collect(Collectors.toList());

        List<TestCaseEntity> savedEntities = springDataRepository.saveAll(entities);

        return savedEntities.stream()
                .map(TestCaseEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TestCase> findById(UUID testCaseId) {
        return springDataRepository.findById(testCaseId)
                .map(TestCaseEntityMapper::toDomain);
    }

    @Override
    public List<TestCase> findAllByProblemId(UUID problemId) {
        return springDataRepository.findByProblemIdOrderByOrderIndexAsc(problemId).stream()
                .map(TestCaseEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID testCaseId) {
        springDataRepository.deleteById(testCaseId);
    }

    @Override
    @Transactional
    public void updateOrderIndexes(Map<UUID, Integer> newOrders) {
        if (newOrders == null || newOrders.isEmpty()) {
            return;
        }
        List<TestCaseEntity> entities = springDataRepository.findAllById(newOrders.keySet());
        for (TestCaseEntity entity : entities) {
            Integer newOrderIndex = newOrders.get(entity.getId());
            if (newOrderIndex != null) {
                entity.setOrderIndex(newOrderIndex);
            }
        }
        springDataRepository.saveAll(entities);
    }
}