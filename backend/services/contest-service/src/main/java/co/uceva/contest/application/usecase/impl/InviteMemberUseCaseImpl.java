package co.uceva.contest.application.usecase.impl;

import co.uceva.contest.application.usecase.InviteMemberUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.InvalidInvitationException;
import co.uceva.contest.domain.exception.InvitationAccessDeniedException;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.repository.InvitationRepository;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.domain.service.EmailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la invitación y avisa al invitado por correo. Si el correo falla se revierte
 * la invitación, para que el capitán no crea que ya fue notificado.
 */
@Service
public class InviteMemberUseCaseImpl implements InviteMemberUseCase {

    private final TeamRepository teamRepository;
    private final ContestRepository contestRepository;
    private final InvitationRepository invitationRepository;
    private final EmailSender emailSender;

    public InviteMemberUseCaseImpl(TeamRepository teamRepository, ContestRepository contestRepository,
                                   InvitationRepository invitationRepository, EmailSender emailSender) {
        this.teamRepository = teamRepository;
        this.contestRepository = contestRepository;
        this.invitationRepository = invitationRepository;
        this.emailSender = emailSender;
    }

    @Override
    @Transactional
    public Invitation execute(InviteMemberCommand command) {
        Team team = teamRepository.findById(command.teamId())
                .orElseThrow(() -> new TeamNotFoundException(command.teamId()));
        if (!team.isCaptain(command.captainUserId())) {
            throw new InvitationAccessDeniedException("Solo el capitán del equipo puede invitar integrantes.");
        }

        Contest contest = contestRepository.findById(team.getContestId())
                .orElseThrow(() -> new ContestNotFoundException(team.getContestId()));
        contest.assertRegistrationOpen();

        if (team.isFull()) {
            throw new MaxTeamMembersReachedException(Team.DEFAULT_MAX_MEMBERS);
        }
        if (teamRepository.existsByContestIdAndUserId(team.getContestId(), command.invitedUserId())) {
            throw new MemberAlreadyInTeamException(command.invitedUserId(), team.getContestId());
        }
        if (invitationRepository.existsPendingByTeamIdAndInvitedUserId(team.getId(), command.invitedUserId())) {
            throw new InvalidInvitationException("El usuario ya tiene una invitación pendiente para este equipo.");
        }

        Invitation invitation = invitationRepository.save(
                Invitation.create(team.getId(), command.invitedUserId(), command.invitedEmail()));
        // shortcut: el envío SMTP ocurre dentro de la transacción, mover a un evento si el correo se vuelve lento.
        emailSender.sendInvitation(invitation.getInvitedEmail(), team.getTeamName().value(),
                contest.getTitle(), invitation.getId());
        return invitation;
    }
}
