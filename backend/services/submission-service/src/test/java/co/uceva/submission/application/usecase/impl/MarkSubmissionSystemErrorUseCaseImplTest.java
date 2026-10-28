package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkSubmissionSystemErrorUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MarkSubmissionSystemErrorUseCaseImpl useCase;

    /**
     * Sin este cierre, un envío que el juez nunca consigue evaluar se quedaría
     * indefinidamente aparentando estar en cola.
     */
    @Test
    void shouldCloseTheSubmissionWithSystemErrorWhenRetriesAreExhausted() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        Submission result = useCase.execute(submission.getId(), "el juez agotó los reintentos");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
        verify(submissionRepository).save(submission);
    }

    /**
     * El veredicto se deja intacto: el juez nunca llegó a pronunciarse, y culpar al
     * código del estudiante de un fallo de la plataforma sería una respuesta falsa.
     */
    @Test
    void shouldLeaveTheVerdictUntouched() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        Submission result = useCase.execute(submission.getId(), "fallo del sandbox");

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.PENDING);
    }

    /** El cambio se señala para que el estudiante deje de esperar. */
    @Test
    void shouldSignalTheStatusChangeSoItCanBeNotified() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        useCase.execute(submission.getId(), "el juez agotó los reintentos");

        ArgumentCaptor<SubmissionStatusChangedEvent> event =
                ArgumentCaptor.forClass(SubmissionStatusChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().submission().getStatus()).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
    }

    /**
     * Un mensaje averiado que cae tarde en la cola de fallidos no puede borrar un
     * resultado válido que el estudiante ya vio en pantalla.
     */
    @Test
    void shouldNotOverwriteASubmissionThatWasAlreadyJudged() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, VerdictStatus.ACCEPTED, SubmissionStatus.JUDGED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        Submission result = useCase.execute(submission.getId(), "mensaje fallido tardío");

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.JUDGED);
        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionStatusChangedEvent.class));
    }

    @Test
    void shouldFailWhenTheSubmissionDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(submissionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(unknownId, "motivo"))
                .isInstanceOf(SubmissionNotFoundException.class);

        verify(submissionRepository, never()).save(any());
    }
}
