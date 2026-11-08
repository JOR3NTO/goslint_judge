package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.UUID;

import co.uceva.contests_platform.contest.application.usecase.GetInvitationByIdUseCase;
import co.uceva.contests_platform.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Invitation;
import co.uceva.contests_platform.contest.domain.repository.InvitationRepository;
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
