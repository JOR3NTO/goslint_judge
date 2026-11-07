package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Invitation;

/** Puerto de entrada: consulta una invitación por su identificador. */
public interface GetInvitationByIdUseCase {

    Invitation execute(UUID invitationId);
}
