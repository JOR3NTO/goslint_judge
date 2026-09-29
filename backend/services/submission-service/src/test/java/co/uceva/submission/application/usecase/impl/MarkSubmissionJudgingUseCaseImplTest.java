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
class MarkSubmissionJudgingUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MarkSubmissionJudgingUseCaseImpl useCase;

    /** Sin esto, un envío se queda aparentando estar en cola durante toda la evaluación. */
    @Test
    void shouldMarkTheSubmissionAsJudging() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        Submission result = useCase.execute(submission.getId());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.JUDGING);
        verify(submissionRepository).save(submission);
    }

    /** El cambio se señala para que el estudiante vea que su envío ya se está evaluando. */
    @Test
    void shouldSignalTheStatusChangeSoItCanBeNotified() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        useCase.execute(submission.getId());

        ArgumentCaptor<SubmissionStatusChangedEvent> event =
                ArgumentCaptor.forClass(SubmissionStatusChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().submission().getStatus()).isEqualTo(SubmissionStatus.JUDGING);
    }

    /**
     * Un aviso de inicio que llega tarde no puede borrar un resultado válido que
     * el estudiante ya vio en pantalla.
     */
    @Test
    void shouldNotOverwriteASubmissionThatWasAlreadyJudged() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, VerdictStatus.ACCEPTED, SubmissionStatus.JUDGED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        Submission result = useCase.execute(submission.getId());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.JUDGED);
        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionStatusChangedEvent.class));
    }

    /** Ni tampoco puede resucitar un envío ya cerrado por un fallo del sistema. */
    @Test
    void shouldNotOverwriteASubmissionThatAlreadyHasASystemError() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.SYSTEM_ERROR);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        Submission result = useCase.execute(submission.getId());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionStatusChangedEvent.class));
    }

    @Test
    void shouldFailWhenTheSubmissionDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(submissionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(unknownId))
                .isInstanceOf(SubmissionNotFoundException.class);

        verify(submissionRepository, never()).save(any());
    }
}
