package co.uceva.contestsplatform.contestmodule.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Invitation;
import co.uceva.contestsplatform.contestmodule.domain.model.InvitationStatus;
import co.uceva.contestsplatform.contestmodule.domain.repository.InvitationRepository;
import co.uceva.contestsplatform.contestmodule.infrastructure.mapper.InvitationEntityMapper;
import co.uceva.contestsplatform.contestmodule.infrastructure.persistence.repository.SpringDataInvitationRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link InvitationRepository}.
 */
@Component
public class InvitationRepositoryImpl implements InvitationRepository {

    private final SpringDataInvitationRepository springDataRepository;

    public InvitationRepositoryImpl(SpringDataInvitationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public Invitation save(Invitation invitation) {
        return InvitationEntityMapper.toDomain(springDataRepository.save(InvitationEntityMapper.toEntity(invitation)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Invitation> findById(UUID invitationId) {
        return springDataRepository.findById(invitationId).map(InvitationEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<Invitation> findAllByInvitedUserId(UUID userId) {
        return springDataRepository.findByInvitedUserIdOrderByCreatedAtDesc(userId).stream()
                .map(InvitationEntityMapper::toDomain).toList();
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsPendingByTeamIdAndInvitedUserId(UUID teamId, UUID invitedUserId) {
        return springDataRepository.existsByTeamIdAndInvitedUserIdAndStatus(
                teamId, invitedUserId, InvitationStatus.PENDING);
    }
}
