package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;

/**
 * Mapper encargado de convertir entre la entidad de dominio {@link Problem}
 * y la entidad JPA {@link ProblemEntity}.
 */
public class ProblemEntityMapper {

    /**
     * Convierte una entidad de dominio en su representación JPA.
     *
     * @param domain Entidad de dominio.
     * @return Entidad JPA o null si el dominio es null.
     */
    public static ProblemEntity toEntity(Problem domain) {
        if (domain == null) return null;

        return ProblemEntity.builder()
                .id(domain.getId())
                .createdBy(domain.getCreatedBy())
                .title(domain.getTitle())
                .statement(domain.getStatement())
                .timeLimitMs(domain.getTimeLimitMs())
                .memoryLimitKb(domain.getMemoryLimitKb())
                .difficult(domain.getDifficult())
                .createdAt(domain.getCreatedAt())
                .inputFormat(domain.getInputFormat())
                .outputFormat(domain.getOutputFormat())
                .build();
    }

    /**
     * Convierte una entidad JPA en su entidad de dominio.
     *
     * @param entity Entidad JPA.
     * @return Entidad de dominio o null si la entidad es null.
     */
    public static Problem toDomain(ProblemEntity entity) {
        if (entity == null) return null;

        return Problem.builder()
                .id(entity.getId())
                .createdBy(entity.getCreatedBy())
                .title(entity.getTitle())
                .statement(entity.getStatement())
                .timeLimitMs(entity.getTimeLimitMs())
                .memoryLimitKb(entity.getMemoryLimitKb())
                .difficult(entity.getDifficult())
                .createdAt(entity.getCreatedAt())
                .inputFormat(entity.getInputFormat())
                .outputFormat(entity.getOutputFormat())
                .build();
    }
}
