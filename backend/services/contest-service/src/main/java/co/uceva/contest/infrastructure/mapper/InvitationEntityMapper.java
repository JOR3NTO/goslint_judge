package co.uceva.contest.infrastructure.mapper;

import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.infrastructure.persistence.entity.InvitationEntity;

/**
 * Mapper entre la entidad de dominio {@link Invitation} y la entidad JPA {@link InvitationEntity}.
 */
public class InvitationEntityMapper {

    public static InvitationEntity toEntity(Invitation domain) {
        return InvitationEntity.builder()
                .id(domain.getId())
                .teamId(domain.getTeamId())
                .invitedUserId(domain.getInvitedUserId())
                .invitedEmail(domain.getInvitedEmail())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .respondedAt(domain.getRespondedAt())
                .build();
    }

    public static Invitation toDomain(InvitationEntity entity) {
        return Invitation.builder()
                .id(entity.getId())
                .teamId(entity.getTeamId())
                .invitedUserId(entity.getInvitedUserId())
                .invitedEmail(entity.getInvitedEmail())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .respondedAt(entity.getRespondedAt())
                .build();
    }
}
