package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import co.uceva.contest.application.usecase.RejectInvitationUseCase;
import co.uceva.contest.domain.exception.InvitationAccessDeniedException;
import co.uceva.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectInvitationUseCaseImpl implements RejectInvitationUseCase {

    private final InvitationRepository invitationRepository;

    public RejectInvitationUseCaseImpl(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    @Transactional
    public void execute(UUID invitationId, UUID userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new InvitationAccessDeniedException("La invitación no está dirigida a este usuario.");
        }
        invitation.reject();
        invitationRepository.save(invitation);
    }
}
