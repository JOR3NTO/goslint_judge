package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.Invitation;

/** Puerto de entrada: el capitán invita a un estudiante a su equipo. */
public interface InviteMemberUseCase {

    Invitation execute(InviteMemberCommand command);

    record InviteMemberCommand(
        UUID teamId,
        UUID captainUserId,
        UUID invitedUserId,
        String invitedEmail
    ){}
}
