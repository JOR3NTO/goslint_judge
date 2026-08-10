package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;

public class TestCaseEntityMapper {

    public static TestCaseEntity toEntity(TestCase domain) {
        if (domain == null) return null;

        return TestCaseEntity.builder()
                .id(domain.getId())
                .problemId(domain.getProblemId())
                .input(domain.getInput())
                .output(domain.getOutput())
                .expectedOutput(domain.getExpectedOutput())
                .orderIndex(domain.getOrderIndex())
                .isSample(domain.isSample())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public static TestCase toDomain(TestCaseEntity entity) {
        if (entity == null) return null;

        return TestCase.builder()
                .id(entity.getId())
                .problemId(entity.getProblemId())
                .expectedOutput(entity.getExpectedOutput())
                .orderIndex(entity.getOrderIndex())
                .isSample(entity.isSample())
                .input(entity.getInput())
                .output(entity.getOutput())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
