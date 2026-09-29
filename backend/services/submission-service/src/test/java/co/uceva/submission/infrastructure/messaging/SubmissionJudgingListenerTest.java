package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionJudgingStartedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionJudgingUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionJudgingListenerTest {

    @Mock private MarkSubmissionJudgingUseCase useCase;
    @InjectMocks private SubmissionJudgingListener listener;

    private SubmissionJudgingStartedEvent event;

    @BeforeEach
    void setUp() {
        event = new SubmissionJudgingStartedEvent(UUID.randomUUID(), Instant.now());
    }

    @Test
    void shouldDelegateToUseCase() {
        listener.onSubmissionJudgingStarted(event);

        verify(useCase).execute(event.submissionId());
    }

    @Test
    void shouldRejectWithoutRequeueWhenTheSubmissionDoesNotExist() {
        when(useCase.execute(event.submissionId())).thenThrow(new SubmissionNotFoundException(event.submissionId()));

        assertThatThrownBy(() -> listener.onSubmissionJudgingStarted(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void shouldPropagateTransientFailuresSoTheyAreRetried() {
        when(useCase.execute(event.submissionId())).thenThrow(new RuntimeException("la base de datos no responde"));

        assertThatThrownBy(() -> listener.onSubmissionJudgingStarted(event))
                .isInstanceOf(RuntimeException.class);
    }
}
