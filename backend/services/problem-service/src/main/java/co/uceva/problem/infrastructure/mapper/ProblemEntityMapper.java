package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;

public class ProblemEntityMapper {

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
