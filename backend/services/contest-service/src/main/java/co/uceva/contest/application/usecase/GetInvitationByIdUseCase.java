package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Invitation;

/** Puerto de entrada: consulta una invitación por su identificador. */
public interface GetInvitationByIdUseCase {

    Invitation execute(UUID invitationId);
}
