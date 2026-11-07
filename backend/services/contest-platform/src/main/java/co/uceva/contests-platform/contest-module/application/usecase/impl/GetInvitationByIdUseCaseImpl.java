package co.uceva.contestsplatform.contestmodule.application.usecase.impl;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.application.usecase.GetInvitationByIdUseCase;
import co.uceva.contestsplatform.contestmodule.domain.exception.InvitationNotFoundException;
import co.uceva.contestsplatform.contestmodule.domain.model.Invitation;
import co.uceva.contestsplatform.contestmodule.domain.repository.InvitationRepository;
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
