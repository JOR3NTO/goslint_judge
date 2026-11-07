package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Team;

/** Puerto de entrada: el invitado acepta la invitación y queda como miembro del equipo. */
public interface AcceptInvitationUseCase {

    Team execute(UUID invitationId, UUID userId);
}
