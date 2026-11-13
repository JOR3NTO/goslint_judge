package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.Team;

/** Puerto de entrada: el invitado acepta la invitación y queda como miembro del equipo. */
public interface AcceptInvitationUseCase {

    Team execute(UUID invitationId, UUID userId);
}
