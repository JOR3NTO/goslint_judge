package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase.UpdateSubmissionVerdictCommand;
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
class UpdateSubmissionVerdictUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private UpdateSubmissionVerdictUseCaseImpl useCase;

    /** El criterio de aceptación en su forma más literal: veredicto, tiempo y memoria. */
    @Test
    void shouldRecordVerdictWithExecutionTimeAndMemoryUsed() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        Submission result = useCase.execute(new UpdateSubmissionVerdictCommand(
                submission.getId(), VerdictStatus.ACCEPTED, 120, 2048));

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(result.getExecutionTimeMs()).isEqualTo(120);
        assertThat(result.getMemoryUsedKb()).isEqualTo(2048);
        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.JUDGED);
        verify(submissionRepository).save(submission);
    }

    /**
     * La notificación se señala, no se envía: quien la reciba lo hará después del
     * commit, de modo que nadie vea en pantalla un veredicto que la base de datos
     * termine descartando.
     */
    @Test
    void shouldSignalTheStatusChangeSoItCanBeNotifiedAfterCommit() {
        Submission submission = SubmissionFixtures.aSubmission();
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissionRepository.save(submission)).thenReturn(submission);

        useCase.execute(new UpdateSubmissionVerdictCommand(
                submission.getId(), VerdictStatus.WRONG_ANSWER, 50, 512));

        ArgumentCaptor<SubmissionStatusChangedEvent> event =
                ArgumentCaptor.forClass(SubmissionStatusChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().submission().getVerdict()).isEqualTo(VerdictStatus.WRONG_ANSWER);
        assertThat(event.getValue().submission().getId()).isEqualTo(submission.getId());
    }

    /** Un veredicto sobre un envío inexistente no se inventa una fila nueva. */
    @Test
    void shouldFailWhenTheJudgedSubmissionDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(submissionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateSubmissionVerdictCommand(
                unknownId, VerdictStatus.ACCEPTED, 10, 10)))
                .isInstanceOf(SubmissionNotFoundException.class);

        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionStatusChangedEvent.class));
    }
}
