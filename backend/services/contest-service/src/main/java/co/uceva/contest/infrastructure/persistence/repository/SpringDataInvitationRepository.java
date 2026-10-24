package co.uceva.contest.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.InvitationStatus;
import co.uceva.contest.infrastructure.persistence.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data JPA para la entidad {@link InvitationEntity}.
 */
public interface SpringDataInvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    List<InvitationEntity> findByInvitedUserIdOrderByCreatedAtDesc(UUID invitedUserId);

    boolean existsByTeamIdAndInvitedUserIdAndStatus(UUID teamId, UUID invitedUserId, InvitationStatus status);
}
