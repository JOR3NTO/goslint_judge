package co.uceva.judge.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import co.uceva.judge.application.usecase.EvaluateSubmissionUseCase;
import co.uceva.judge.domain.exception.SandboxExecutionException;
import co.uceva.judge.domain.exception.TestCasesNotFoundException;
import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionEvaluationListenerTest {

    @Mock private EvaluateSubmissionUseCase useCase;
    @InjectMocks private SubmissionEvaluationListener listener;

    private SubmissionReceivedEvent event;

    @BeforeEach
    void setUp() {
        event = new SubmissionReceivedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ProgrammingLanguage.PYTHON, "print(1)", Instant.now());
    }

    @Test
    void shouldDelegateToUseCase() {
        listener.onSubmissionReceived(event);

        verify(useCase).execute(event);
    }

    @Test
    void shouldRejectWithoutRequeueWhenTestCasesAreMissing() {
        when(useCase.execute(event)).thenThrow(new TestCasesNotFoundException(event.problemId()));

        assertThatThrownBy(() -> listener.onSubmissionReceived(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void shouldRejectWithoutRequeueWhenEventIsInvalid() {
        when(useCase.execute(event)).thenThrow(new IllegalArgumentException("inválido"));

        assertThatThrownBy(() -> listener.onSubmissionReceived(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void shouldPropagateTransientFailuresSoTheyAreRetried() {
        when(useCase.execute(event)).thenThrow(new SandboxExecutionException("fallo", null));

        assertThatThrownBy(() -> listener.onSubmissionReceived(event))
                .isInstanceOf(SandboxExecutionException.class);
    }
}
