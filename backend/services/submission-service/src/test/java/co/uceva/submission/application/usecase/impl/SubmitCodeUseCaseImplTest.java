package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.event.SubmissionPersistedEvent;
import co.uceva.submission.application.exception.ContestServiceUnavailableException;
import co.uceva.submission.application.port.out.ContestDataPort;
import co.uceva.submission.application.usecase.SubmitCodeUseCase.SubmitCodeCommand;
import co.uceva.submission.domain.model.ContestSnapshot;
import co.uceva.submission.domain.model.SubmissionRejectionReason;
import co.uceva.submission.domain.model.TeamSnapshot;
import co.uceva.submission.domain.exception.DuplicateSubmissionException;
import co.uceva.submission.domain.exception.SubmissionNotAllowedException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Cubre el caso de uso de envío junto con la política de elegibilidad que aplica:
 * el puerto de {@code contest-service} se simula y el reloj está fijo.
 */
@ExtendWith(MockitoExtension.class)
class SubmitCodeUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2025-06-01T12:00:00Z");

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ContestDataPort contestDataPort;

    private SubmitCodeUseCaseImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID contestId = UUID.randomUUID();
    private final UUID problemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new SubmitCodeUseCaseImpl(submissionRepository, applicationEventPublisher, contestDataPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // --- Ayudantes ---

    private SubmitCodeCommand command(UUID submitter) {
        return new SubmitCodeCommand(teamId, problemId, submitter, ProgrammingLanguage.PYTHON, "print(1)");
    }

    private ContestSnapshot contest(Instant start, Instant end) {
        return new ContestSnapshot(contestId, start, end);
    }

    private ContestSnapshot activeContest() {
        return contest(NOW.minusSeconds(60), NOW.plusSeconds(60));
    }

    private void givenTeamWithMember(UUID member) {
        when(contestDataPort.findTeam(teamId))
                .thenReturn(Optional.of(new TeamSnapshot(teamId, contestId, List.of(member))));
    }

    private void givenActiveContestWithTheProblem() {
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.of(activeContest()));
        when(contestDataPort.findContestProblemIds(contestId)).thenReturn(List.of(problemId));
    }

    private void givenSaveReturnsArgument() {
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void assertRejected(SubmitCodeCommand command, SubmissionRejectionReason reason) {
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(SubmissionNotAllowedException.class)
                .extracting(e -> ((SubmissionNotAllowedException) e).getReason())
                .isEqualTo(reason);
        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionPersistedEvent.class));
    }

    // --- Camino feliz y duplicados ---

    @Test
    void shouldCreateSubmissionForAnEligibleMember() {
        givenTeamWithMember(userId);
        givenActiveContestWithTheProblem();
        givenSaveReturnsArgument();

        Submission result = useCase.execute(command(userId));

        assertThat(result.getTeamId()).isEqualTo(teamId);
        assertThat(result.getProblemId()).isEqualTo(problemId);
        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        verify(applicationEventPublisher).publishEvent(new SubmissionPersistedEvent(result));
    }

    @Test
    void shouldThrowWhenDuplicateExistsAfterPassingEligibility() {
        givenTeamWithMember(userId);
        givenActiveContestWithTheProblem();
        when(submissionRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                teamId, problemId, "print(1)", ProgrammingLanguage.PYTHON)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command(userId))).isInstanceOf(DuplicateSubmissionException.class);

        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionPersistedEvent.class));
    }

    @Test
    void shouldNotCheckDuplicatesWhenTheSubmissionIsNotEligible() {
        when(contestDataPort.findTeam(teamId)).thenReturn(Optional.empty());

        assertRejected(command(userId), SubmissionRejectionReason.TEAM_NOT_FOUND);

        verifyNoInteractions(submissionRepository);
    }

    // --- Sin exenciones: la regla no mira el rol ---

    @Test
    void shouldRejectAnAdminOrOrganizerWhoIsNotAMemberOfTheTeam() {
        // El comando no lleva rol: un administrador u organizador ajeno al equipo
        // se trata igual que cualquier otro usuario que no sea integrante.
        givenTeamWithMember(UUID.randomUUID());

        assertRejected(command(UUID.randomUUID()), SubmissionRejectionReason.NOT_A_MEMBER);
    }

    // --- Motivos de rechazo ---

    @Test
    void shouldRejectWhenTheTeamDoesNotExist() {
        when(contestDataPort.findTeam(teamId)).thenReturn(Optional.empty());

        assertRejected(command(userId), SubmissionRejectionReason.TEAM_NOT_FOUND);
    }

    @Test
    void shouldRejectAndStopEarlyWhenTheUserIsNotAMember() {
        givenTeamWithMember(UUID.randomUUID());

        assertRejected(command(userId), SubmissionRejectionReason.NOT_A_MEMBER);

        verify(contestDataPort).findTeam(teamId);
        verifyNoMoreInteractions(contestDataPort);
    }

    @Test
    void shouldRejectWhenTheTeamContestDoesNotExist() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.empty());

        assertRejected(command(userId), SubmissionRejectionReason.CONTEST_NOT_FOUND);
    }

    @Test
    void shouldRejectWhenTheContestHasNotStarted() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId))
                .thenReturn(Optional.of(contest(NOW.plusSeconds(1), NOW.plusSeconds(100))));

        assertRejected(command(userId), SubmissionRejectionReason.CONTEST_NOT_ACTIVE);

        verify(contestDataPort, never()).findContestProblemIds(any());
    }

    @Test
    void shouldRejectWhenTheContestHasEnded() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId))
                .thenReturn(Optional.of(contest(NOW.minusSeconds(100), NOW.minusSeconds(1))));

        assertRejected(command(userId), SubmissionRejectionReason.CONTEST_NOT_ACTIVE);
    }

    @Test
    void shouldAcceptASubmissionExactlyAtTheContestStart() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.of(contest(NOW, NOW.plusSeconds(100))));
        when(contestDataPort.findContestProblemIds(contestId)).thenReturn(List.of(problemId));
        givenSaveReturnsArgument();

        assertThat(useCase.execute(command(userId)).getTeamId()).isEqualTo(teamId);
    }

    @Test
    void shouldRejectASubmissionExactlyAtTheContestEnd() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.of(contest(NOW.minusSeconds(100), NOW)));

        assertRejected(command(userId), SubmissionRejectionReason.CONTEST_NOT_ACTIVE);
    }

    @Test
    void shouldRejectWhenTheProblemIsNotInTheContest() {
        givenTeamWithMember(userId);
        when(contestDataPort.findContest(contestId)).thenReturn(Optional.of(activeContest()));
        when(contestDataPort.findContestProblemIds(contestId)).thenReturn(List.of(UUID.randomUUID()));

        assertRejected(command(userId), SubmissionRejectionReason.PROBLEM_NOT_IN_CONTEST);
    }

    // --- Contest-service caído ---

    @Test
    void shouldPropagateContestServiceUnavailable() {
        when(contestDataPort.findTeam(teamId))
                .thenThrow(new ContestServiceUnavailableException("timeout", new RuntimeException()));

        assertThatThrownBy(() -> useCase.execute(command(userId)))
                .isInstanceOf(ContestServiceUnavailableException.class);

        verify(submissionRepository, never()).save(any());
    }
}
