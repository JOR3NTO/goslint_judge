package co.uceva.contest.infrastructure.mapper;

import co.uceva.contest.domain.model.Member;
import co.uceva.contest.infrastructure.persistence.entity.MemberEntity;

/**
 * Mapper entre la entidad de dominio {@link Member} y la entidad JPA {@link MemberEntity}.
 */
public class MemberEntityMapper {

    public static MemberEntity toEntity(Member domain) {
        if (domain == null) return null;

        return MemberEntity.builder()
                .id(domain.getId())
                .teamId(domain.getTeamId())
                .userId(domain.getUserId())
                .registeredAt(domain.getRegisteredAt())
                .build();
    }

    public static Member toDomain(MemberEntity entity) {
        if (entity == null) return null;

        return Member.builder()
                .id(entity.getId())
                .teamId(entity.getTeamId())
                .userId(entity.getUserId())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }
}
