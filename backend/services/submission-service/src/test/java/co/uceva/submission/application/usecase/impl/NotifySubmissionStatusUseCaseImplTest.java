package co.uceva.submission.application.usecase.impl;

import co.uceva.submission.application.port.out.SubmissionStatusNotifier;
import co.uceva.submission.application.port.out.TeamDTO;
import co.uceva.submission.application.port.out.TeamMembershipPort;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifySubmissionStatusUseCaseImplTest {

    @Mock
    private TeamMembershipPort teamMembershipPort;

    @Mock
    private SubmissionStatusNotifier submissionStatusNotifier;

    @InjectMocks
    private NotifySubmissionStatusUseCaseImpl useCase;

    /**
     * La comprobación que sostiene la privacidad del canal: los destinatarios salen
     * del equipo dueño del envío y de ninguna otra parte.
     */
    @Test
    void shouldNotifyOnlyTheMembersOfTheOwningTeam() {
        Submission submission = SubmissionFixtures.aSubmission();
        UUID primerIntegrante = UUID.randomUUID();
        UUID segundoIntegrante = UUID.randomUUID();
        when(teamMembershipPort.findTeam(submission.getTeamId()))
                .thenReturn(new TeamDTO(submission.getTeamId(), List.of(primerIntegrante, segundoIntegrante)));

        useCase.execute(submission);

        ArgumentCaptor<List<UUID>> recipients = ArgumentCaptor.forClass(List.class);
        verify(submissionStatusNotifier).notifyStatusChanged(any(Submission.class), recipients.capture());
        assertThat(recipients.getValue()).containsExactly(primerIntegrante, segundoIntegrante);
    }

    /**
     * Un equipo cuyos integrantes no se pueden determinar no es un error: no hay a
     * quién avisar, y el estado sigue disponible por HTTP.
     */
    @Test
    void shouldNotNotifyAnyoneWhenTheTeamHasNoKnownMembers() {
        Submission submission = SubmissionFixtures.aSubmission();
        when(teamMembershipPort.findTeam(submission.getTeamId()))
                .thenReturn(new TeamDTO(submission.getTeamId(), List.of()));

        useCase.execute(submission);

        verify(submissionStatusNotifier, never()).notifyStatusChanged(any(), anyList());
    }
}
