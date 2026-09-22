package co.uceva.submission.application.usecase.impl;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.application.exception.EventPublishingException;
import co.uceva.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnqueueSubmissionUseCaseImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SubmissionEventPublisher submissionEventPublisher;

    @InjectMocks
    private EnqueueSubmissionUseCaseImpl useCase;

    @Test
    void shouldMarkSubmissionAsQueuedWhenDeliveryIsConfirmed() {
        Submission submission = SubmissionFixtures.aSubmission();

        useCase.execute(submission);

        verify(submissionEventPublisher).publishSubmissionReceived(submission);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.QUEUED);
        verify(submissionRepository).save(submission);
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
