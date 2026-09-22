package co.uceva.submission;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.port.out.SubmissionStatusNotifier;
import co.uceva.submission.application.port.out.TeamDTO;
import co.uceva.submission.application.port.out.TeamMembershipPort;
import co.uceva.submission.application.usecase.MarkSubmissionSystemErrorUseCase;
import co.uceva.submission.application.usecase.SubmitCodeUseCase;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase.UpdateSubmissionVerdictCommand;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recorre el camino completo desde que el juez devuelve un resultado hasta que la
 * notificación sale hacia los interesados.
 * <p>
 * Es lo que las pruebas unitarias no pueden cubrir: que el evento interno, el
 * listener transaccional y el puerto de notificación estén realmente conectados
 * entre sí dentro del contexto de Spring. El canal WebSocket se sustituye por un
 * simulacro del puerto, porque lo que se comprueba aquí es el cableado, no el
 * transporte.
 * </p>
 */
@SpringBootTest
class SubmissionVerdictNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SubmitCodeUseCase submitCodeUseCase;

    @Autowired
    private UpdateSubmissionVerdictUseCase updateSubmissionVerdictUseCase;

    @Autowired
    private MarkSubmissionSystemErrorUseCase markSubmissionSystemErrorUseCase;

    @Autowired
    private SubmissionRepository submissionRepository;

    @MockBean
    private SubmissionStatusNotifier submissionStatusNotifier;

    @MockBean
    private TeamMembershipPort teamMembershipPort;

    /**
     * El criterio de aceptación completo: el veredicto y sus métricas quedan
     * registrados y el cambio sale hacia el dueño del envío sin que nadie recargue
     * nada.
     */
    @Test
    void shouldPersistTheVerdictAndNotifyTheOwnerWhenTheJudgeFinishes() {
        UUID teamId = UUID.randomUUID();
        UUID integrante = UUID.randomUUID();
        when(teamMembershipPort.findTeam(teamId)).thenReturn(new TeamDTO(teamId, List.of(integrante)));
        Submission submission = givenASubmission(teamId);

        updateSubmissionVerdictUseCase.execute(new UpdateSubmissionVerdictCommand(
                submission.getId(), VerdictStatus.ACCEPTED, 120, 2048));

        Submission persisted = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(persisted.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(persisted.getStatus()).isEqualTo(SubmissionStatus.JUDGED);
        assertThat(persisted.getExecutionTimeMs()).isEqualTo(120);
        assertThat(persisted.getMemoryUsedKb()).isEqualTo(2048);

        ArgumentCaptor<Submission> notificado = ArgumentCaptor.forClass(Submission.class);
        ArgumentCaptor<List<UUID>> destinatarios = ArgumentCaptor.forClass(List.class);
        verify(submissionStatusNotifier).notifyStatusChanged(notificado.capture(), destinatarios.capture());
        assertThat(notificado.getValue().getId()).isEqualTo(submission.getId());
        assertThat(notificado.getValue().getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(destinatarios.getValue()).containsExactly(integrante);
    }

    /**
     * El tercer criterio de aceptación: agotados los reintentos, el envío no se
     * queda colgado y su nuevo estado viaja por el mismo canal.
     */
    @Test
    void shouldMarkSystemErrorAndNotifyItThroughTheSameChannel() {
        UUID teamId = UUID.randomUUID();
        UUID integrante = UUID.randomUUID();
        when(teamMembershipPort.findTeam(teamId)).thenReturn(new TeamDTO(teamId, List.of(integrante)));
        Submission submission = givenASubmission(teamId);

        markSubmissionSystemErrorUseCase.execute(submission.getId(), "el juez agotó los reintentos");

        Submission persisted = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
        assertThat(persisted.getVerdict()).isEqualTo(VerdictStatus.PENDING);

        ArgumentCaptor<Submission> notificado = ArgumentCaptor.forClass(Submission.class);
        verify(submissionStatusNotifier).notifyStatusChanged(notificado.capture(), anyList());
        assertThat(notificado.getValue().getStatus()).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
    }

    /**
     * Si no se sabe a quién pertenece el envío, no se notifica a nadie. Es la
     * postura segura: ante la duda, callar antes que difundir.
     */
    @Test
    void shouldNotifyNobodyWhenTheOwningTeamHasNoKnownMembers() {
        UUID teamId = UUID.randomUUID();
        when(teamMembershipPort.findTeam(teamId)).thenReturn(new TeamDTO(teamId, List.of()));
        Submission submission = givenASubmission(teamId);

        updateSubmissionVerdictUseCase.execute(new UpdateSubmissionVerdictCommand(
                submission.getId(), VerdictStatus.WRONG_ANSWER, 30, 256));

        verify(submissionStatusNotifier, never()).notifyStatusChanged(any(), anyList());
    }

    private Submission givenASubmission(UUID teamId) {
        return submitCodeUseCase.execute(new SubmitCodeUseCase.SubmitCodeCommand(
                teamId, UUID.randomUUID(), ProgrammingLanguage.PYTHON, "print(" + UUID.randomUUID() + ")"));
    }
}
