package co.uceva.contest.infrastructure.mapper;

import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.valueobject.BalloonColor;
import co.uceva.contest.infrastructure.persistence.entity.ContestProblemEntity;

/**
 * Mapper entre la entidad de dominio {@link ContestProblem} y la entidad JPA {@link ContestProblemEntity}.
 */
public class ContestProblemEntityMapper {

    public static ContestProblemEntity toEntity(ContestProblem domain) {
        if (domain == null) return null;

        return ContestProblemEntity.builder()
                .id(domain.getId())
                .contestId(domain.getContestId())
                .problemId(domain.getProblemId())
                .alias(domain.getAlias())
                .orderIndex(domain.getOrderIndex())
                .points(domain.getPoints())
                .balloonColor(domain.getBalloonColor().value())
                .build();
    }

    public static ContestProblem toDomain(ContestProblemEntity entity) {
        if (entity == null) return null;

        return ContestProblem.builder()
                .id(entity.getId())
                .contestId(entity.getContestId())
                .problemId(entity.getProblemId())
                .alias(entity.getAlias())
                .orderIndex(entity.getOrderIndex())
                .points(entity.getPoints())
                .balloonColor(BalloonColor.of(entity.getBalloonColor()))
                .build();
    }
}
