package co.uceva.contestsplatform.contestmodule.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.application.usecase.GetInvitationsByUserUseCase;
import co.uceva.contestsplatform.contestmodule.domain.model.Invitation;
import co.uceva.contestsplatform.contestmodule.domain.repository.InvitationRepository;
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
