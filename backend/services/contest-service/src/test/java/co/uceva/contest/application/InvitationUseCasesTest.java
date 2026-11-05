package co.uceva.contest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.application.usecase.InviteMemberUseCase.InviteMemberCommand;
import co.uceva.contest.application.usecase.impl.AcceptInvitationUseCaseImpl;
import co.uceva.contest.application.usecase.impl.InviteMemberUseCaseImpl;
import co.uceva.contest.domain.exception.InvalidInvitationException;
import co.uceva.contest.domain.exception.InvitationAccessDeniedException;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.RegistrationClosedException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.ContestStatus;
import co.uceva.contest.domain.model.Invitation;
import co.uceva.contest.domain.model.InvitationStatus;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.repository.InvitationRepository;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.domain.service.EmailSender;
import co.uceva.contest.domain.valueobject.ContestPeriod;
import co.uceva.contest.domain.valueobject.TeamName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Invitaciones a equipos: capitán, límite de miembros y cierre de inscripción")
class InvitationUseCasesTest {

    private final TeamRepository teams = mock(TeamRepository.class);
    private final ContestRepository contests = mock(ContestRepository.class);
    private final InvitationRepository invitations = mock(InvitationRepository.class);
    private final EmailSender email = mock(EmailSender.class);

    private final UUID captain = UUID.randomUUID();
    private final UUID guest = UUID.randomUUID();
    private Team team;

    private Contest contestStarting(Instant start) {
        return Contest.builder().id(UUID.randomUUID()).title("Maratón")
                .period(ContestPeriod.restore(start, start.plus(2, ChronoUnit.HOURS)))
                .status(ContestStatus.UPCOMING).build();
    }

    private void givenContest(Instant start) {
        when(teams.findById(team.getId())).thenReturn(Optional.of(team));
        when(contests.findById(team.getContestId())).thenReturn(Optional.of(contestStarting(start)));
    }

    private Instant inOneHour() {
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    @BeforeEach
    void setUp() {
        team = Team.create(UUID.randomUUID(), TeamName.of("Los Capitanes"), captain);
        when(invitations.save(any())).thenAnswer(i -> i.getArgument(0));
        when(teams.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private InviteMemberUseCaseImpl inviter() {
        return new InviteMemberUseCaseImpl(teams, contests, invitations, email);
    }

    private AcceptInvitationUseCaseImpl accepter() {
        return new AcceptInvitationUseCaseImpl(invitations, teams, contests);
    }

    @Test
    @DisplayName("El capitán invita y se envía el correo")
    void captainInvites() {
        givenContest(inOneHour());

        Invitation inv = inviter().execute(new InviteMemberCommand(team.getId(), captain, guest, "a@b.co"));

        assertThat(inv.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(email).sendInvitation("a@b.co", "Los Capitanes", "Maratón", inv.getId());
    }

    @Test
    @DisplayName("Quien no es capitán no puede invitar")
    void nonCaptainCannotInvite() {
        givenContest(inOneHour());

        assertThatThrownBy(() -> inviter().execute(new InviteMemberCommand(team.getId(), guest, guest, "a@b.co")))
                .isInstanceOf(InvitationAccessDeniedException.class);
        verify(email, never()).sendInvitation(any(), any(), any(), any());
    }

    @Test
    @DisplayName("No se invita con el equipo lleno")
    void fullTeamCannotInvite() {
        team.addMember(UUID.randomUUID());
        team.addMember(UUID.randomUUID());
        givenContest(inOneHour());

        assertThatThrownBy(() -> inviter().execute(new InviteMemberCommand(team.getId(), captain, guest, "a@b.co")))
                .isInstanceOf(MaxTeamMembersReachedException.class);
    }

    @Test
    @DisplayName("No se invita si el concurso ya inició")
    void startedContestCannotInvite() {
        givenContest(Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThatThrownBy(() -> inviter().execute(new InviteMemberCommand(team.getId(), captain, guest, "a@b.co")))
                .isInstanceOf(RegistrationClosedException.class);
    }

    @Test
    @DisplayName("Al aceptar, el invitado queda como miembro")
    void acceptAddsMember() {
        givenContest(inOneHour());
        Invitation inv = Invitation.create(team.getId(), guest, "a@b.co");
        when(invitations.findById(inv.getId())).thenReturn(Optional.of(inv));

        Team result = accepter().execute(inv.getId(), guest);

        assertThat(result.hasMember(guest)).isTrue();
        assertThat(inv.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Aceptar con el equipo lleno invalida la invitación y explica el motivo")
    void acceptOnFullTeamExpires() {
        team.addMember(UUID.randomUUID());
        team.addMember(UUID.randomUUID());
        givenContest(inOneHour());
        Invitation inv = Invitation.create(team.getId(), guest, "a@b.co");
        when(invitations.findById(inv.getId())).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> accepter().execute(inv.getId(), guest))
                .isInstanceOf(InvalidInvitationException.class)
                .hasMessageContaining("máximo de 3");
        assertThat(inv.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(invitations).save(inv);
        assertThat(team.hasMember(guest)).isFalse();
    }

    @Test
    @DisplayName("Solo el invitado puede aceptar su invitación")
    void onlyInviteeCanAccept() {
        Invitation inv = Invitation.create(team.getId(), guest, "a@b.co");
        when(invitations.findById(inv.getId())).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> accepter().execute(inv.getId(), UUID.randomUUID()))
                .isInstanceOf(InvitationAccessDeniedException.class);
    }
}
