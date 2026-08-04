package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;

public class TestCaseEntityMapper {

    public static TestCaseEntity toEntity(TestCase domain) {
        if (domain == null) return null;

        TestCaseEntity entity = new TestCaseEntity();
        entity.setId(domain.getId());
        entity.setProblemId(domain.getProblemId());
        entity.setInput(domain.getInput());
        entity.setOutput(domain.getOutput());
        entity.setExpectedOutput(domain.getExpectedOutput());
        entity.setOrderIndex(domain.getOrderIndex());
        entity.setSample(domain.isSample());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static TestCase toDomain(TestCaseEntity entity) {
        if (entity == null) return null;

        return new TestCase(
                entity.getId(),
                entity.getProblemId(),
                entity.getExpectedOutput(),
                entity.getOrderIndex(),
                entity.isSample(),
                entity.getInput(),
                entity.getOutput(),
                entity.getCreatedAt()
        );
    }
}