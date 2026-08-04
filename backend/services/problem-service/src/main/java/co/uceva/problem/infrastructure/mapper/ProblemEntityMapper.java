package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;

public class ProblemEntityMapper {

    public static ProblemEntity toEntity(Problem domain) {
        if (domain == null) return null;

        ProblemEntity entity = new ProblemEntity();
        entity.setId(domain.getId());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setTitle(domain.getTitle());
        entity.setStatement(domain.getStatement());
        entity.setTimeLimitMs(domain.getTimeLimitMs());
        entity.setMemoryLimitKb(domain.getMemoryLimitKb());
        entity.setDifficult(domain.getDifficult());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setInputFormat(domain.getInputFormat());
        entity.setOutputFormat(domain.getOutputFormat());
        return entity;
    }

    public static Problem toDomain(ProblemEntity entity) {
        if (entity == null) return null;

        return new Problem(
                entity.getId(),
                entity.getCreatedBy(),
                entity.getTitle(),
                entity.getStatement(),
                entity.getTimeLimitMs(),
                entity.getMemoryLimitKb(),
                entity.getDifficult(),
                entity.getCreatedAt(),
                entity.getInputFormat(),
                entity.getOutputFormat()
        );
    }
}