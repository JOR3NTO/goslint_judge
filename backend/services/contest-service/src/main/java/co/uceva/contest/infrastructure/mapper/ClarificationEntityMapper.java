package co.uceva.contest.infrastructure.mapper;

import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.infrastructure.persistence.entity.ClarificationEntity;

/**
 * Mapper entre la entidad de dominio {@link Clarification} y la entidad JPA {@link ClarificationEntity}.
 */
public class ClarificationEntityMapper {

    public static ClarificationEntity toEntity(Clarification domain) {
        if (domain == null) return null;

        return ClarificationEntity.builder()
                .id(domain.getId())
                .problemId(domain.getProblemId())
                .teamId(domain.getTeamId())
                .question(domain.getQuestion())
                .answer(domain.getAnswer())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .answeredAt(domain.getAnsweredAt())
                .build();
    }

    public static Clarification toDomain(ClarificationEntity entity) {
        if (entity == null) return null;

        return Clarification.builder()
                .id(entity.getId())
                .problemId(entity.getProblemId())
                .teamId(entity.getTeamId())
                .question(entity.getQuestion())
                .answer(entity.getAnswer())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .answeredAt(entity.getAnsweredAt())
                .build();
    }
}
