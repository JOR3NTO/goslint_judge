package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.exception.EventPublishingException;
import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnqueueSubmissionUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionEventPublisher submissionEventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EnqueueSubmissionUseCaseImpl useCase;

    @Test
    void shouldMarkSubmissionAsQueuedWhenDeliveryIsConfirmed() {
        Submission submission = SubmissionFixtures.aSubmission();
        when(submissionRepository.save(submission)).thenReturn(submission);

        useCase.execute(submission);

        verify(submissionEventPublisher).publishSubmissionReceived(submission);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.QUEUED);
        InOrder inOrder = inOrder(submissionRepository, applicationEventPublisher);
        inOrder.verify(submissionRepository).save(submission);
        inOrder.verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof SubmissionStatusChangedEvent changedEvent
                        && changedEvent.submission() == submission
                        && changedEvent.submission().getStatus() == SubmissionStatus.QUEUED));
    }

    @Test
    void shouldKeepSubmissionPendingWhenDeliveryIsNotConfirmed() {
        Submission submission = SubmissionFixtures.aSubmission();
        doThrow(new EventPublishingException(submission.getId(), "el broker no está disponible"))
                .when(submissionEventPublisher).publishSubmissionReceived(submission);

        useCase.execute(submission);

        // El envío sigue en espera y ningún cambio de estado se persiste,
        // de modo que el reintento automático pueda recogerlo más tarde.
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        verify(submissionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(SubmissionStatusChangedEvent.class));
    }

    @Test
    void shouldNotPropagateMessagingFailures() {
        Submission submission = SubmissionFixtures.aSubmission();
        doThrow(new EventPublishingException(submission.getId(), "tiempo de espera agotado"))
                .when(submissionEventPublisher).publishSubmissionReceived(submission);

        assertThatCode(() -> useCase.execute(submission)).doesNotThrowAnyException();
    }

    @Test
    void shouldNotDowngradeSubmissionThatWasAlreadyQueued() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);

        useCase.execute(submission);

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.QUEUED);
    }
}
