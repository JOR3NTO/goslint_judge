package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.Invitation;

/** Puerto de salida para la persistencia de invitaciones. */
public interface InvitationRepository {

    Invitation save(Invitation invitation);

    Optional<Invitation> findById(UUID invitationId);

    List<Invitation> findAllByInvitedUserId(UUID userId);

    boolean existsPendingByTeamIdAndInvitedUserId(UUID teamId, UUID invitedUserId);
}
