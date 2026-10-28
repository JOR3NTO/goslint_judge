package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import co.uceva.contest.application.usecase.AcceptInvitationUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.InvalidInvitationException;
import co.uceva.contest.domain.exception.InvitationAccessDeniedException;
import co.uceva.contest.domain.exception.InvitationNotFoundException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.repository.InvitationRepository;
import co.uceva.contest.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcceptInvitationUseCaseImpl implements AcceptInvitationUseCase {

    private final InvitationRepository invitationRepository;
    private final TeamRepository teamRepository;
    private final ContestRepository contestRepository;

    public AcceptInvitationUseCaseImpl(InvitationRepository invitationRepository, TeamRepository teamRepository,
                                       ContestRepository contestRepository) {
        this.invitationRepository = invitationRepository;
        this.teamRepository = teamRepository;
        this.contestRepository = contestRepository;
    }

    /**
     * Si el equipo ya está lleno la invitación se invalida y se informa al invitado;
     * por eso la excepción no revierte la transacción.
     */
    @Override
    @Transactional(noRollbackFor = InvalidInvitationException.class)
    public Team execute(UUID invitationId, UUID userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new InvitationAccessDeniedException("La invitación no está dirigida a este usuario.");
        }
        if (!invitation.isPending()) {
            throw new InvalidInvitationException("La invitación ya fue respondida o ya no es válida.");
        }

        Team team = teamRepository.findById(invitation.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException(invitation.getTeamId()));
        Contest contest = contestRepository.findById(team.getContestId())
                .orElseThrow(() -> new ContestNotFoundException(team.getContestId()));
        contest.assertRegistrationOpen();

        if (team.isFull()) {
            invitation.expire();
            invitationRepository.save(invitation);
            throw new InvalidInvitationException("La invitación ya no es válida: el equipo alcanzó el máximo de "
                    + Team.DEFAULT_MAX_MEMBERS + " integrantes.");
        }
        if (teamRepository.existsByContestIdAndUserId(team.getContestId(), userId)) {
            throw new MemberAlreadyInTeamException(userId, team.getContestId());
        }

        team.addMember(userId);
        Team saved = teamRepository.save(team);
        invitation.accept();
        invitationRepository.save(invitation);
        return saved;
    }
}
