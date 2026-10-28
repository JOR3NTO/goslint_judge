package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import co.uceva.contest.application.usecase.GetInvitationByIdUseCase;
import co.uceva.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.repository.InvitationRepository;
import org.springframework.stereotype.Service;

@Service
public class GetInvitationByIdUseCaseImpl implements GetInvitationByIdUseCase {

    private final InvitationRepository invitationRepository;

    public GetInvitationByIdUseCaseImpl(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public Invitation execute(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
    }
}
