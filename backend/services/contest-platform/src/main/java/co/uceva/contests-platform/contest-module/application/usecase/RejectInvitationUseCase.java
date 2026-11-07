package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.UUID;

/** Puerto de entrada: el invitado rechaza la invitación. */
public interface RejectInvitationUseCase {

    void execute(UUID invitationId, UUID userId);
}
