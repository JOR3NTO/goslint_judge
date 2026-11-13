package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import co.uceva.contests_platform.contest.application.usecase.GetInvitationsByUserUseCase;
import co.uceva.contests_platform.contest.domain.model.Invitation;
import co.uceva.contests_platform.contest.domain.repository.InvitationRepository;
import org.springframework.stereotype.Service;

@Service
public class GetInvitationsByUserUseCaseImpl implements GetInvitationsByUserUseCase {

    private final InvitationRepository invitationRepository;

    public GetInvitationsByUserUseCaseImpl(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<Invitation> execute(UUID userId) {
        return invitationRepository.findAllByInvitedUserId(userId);
    }
}
