package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;

/**
 * Mapper encargado de convertir entre la entidad de dominio {@link TestCase}
 * y la entidad JPA {@link TestCaseEntity}.
 */
public class TestCaseEntityMapper {

    /**
     * Convierte una entidad de dominio en su representación JPA.
     *
     * @param domain Entidad de dominio.
     * @return Entidad JPA o null si el dominio es null.
     */
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

    /**
     * Convierte una entidad JPA en su entidad de dominio.
     *
     * @param entity Entidad JPA.
     * @return Entidad de dominio o null si la entidad es null.
     */
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
