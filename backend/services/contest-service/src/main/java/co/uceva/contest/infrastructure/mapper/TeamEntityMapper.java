package co.uceva.contest.infrastructure.mapper;

import java.util.ArrayList;

import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.valueobject.TeamName;
import co.uceva.contest.domain.valueobject.TeamScore;
import co.uceva.contest.infrastructure.persistence.entity.TeamEntity;

/**
 * Mapper entre la entidad de dominio {@link Team} y la entidad JPA {@link TeamEntity},
 * incluyendo a sus integrantes.
 */
public class TeamEntityMapper {

    public static TeamEntity toEntity(Team domain) {
        if (domain == null) return null;

        return TeamEntity.builder()
                .id(domain.getId())
                .contestId(domain.getContestId())
                .teamName(domain.getTeamName().value())
                .score(domain.getTeamScore().score())
                .penaltyTime(domain.getTeamScore().penaltyTime())
                .createdAt(domain.getCreatedAt())
                .members(domain.getMembers().stream()
                        .map(MemberEntityMapper::toEntity)
                        .peek(m -> m.setTeamId(domain.getId()))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new)))
                .build();
    }

    public static Team toDomain(TeamEntity entity) {
        if (entity == null) return null;

        return Team.builder()
                .id(entity.getId())
                .contestId(entity.getContestId())
                .teamName(TeamName.of(entity.getTeamName()))
                .teamScore(TeamScore.of(entity.getScore(), entity.getPenaltyTime()))
                .createdAt(entity.getCreatedAt())
                .members(entity.getMembers().stream().map(MemberEntityMapper::toDomain).toList())
                .build();
    }
}
