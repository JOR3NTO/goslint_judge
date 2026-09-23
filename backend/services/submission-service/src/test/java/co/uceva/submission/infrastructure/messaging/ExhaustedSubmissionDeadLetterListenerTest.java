package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.shared.domain.event.SubmissionJudgedEvent;
import co.uceva.shared.domain.event.SubmissionJudgingStartedEvent;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionSystemErrorUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre las tres colas de fallidos que este listener vigila: la evaluación
 * nunca empezó, empezó pero el aviso no se registró, o terminó pero el
 * veredicto no se registró. En los tres casos el desenlace es el mismo:
 * {@code SYSTEM_ERROR}, sin propagar ningún fallo.
 */
@ExtendWith(MockitoExtension.class)
class ExhaustedSubmissionDeadLetterListenerTest {

    @Mock private MarkSubmissionSystemErrorUseCase markSubmissionSystemErrorUseCase;
    @InjectMocks private ExhaustedSubmissionDeadLetterListener listener;

    @Test
    void shouldMarkSystemErrorWhenEvaluationRetriesAreExhausted() {
        UUID submissionId = UUID.randomUUID();
        SubmissionReceivedEvent event = new SubmissionReceivedEvent(submissionId, UUID.randomUUID(),
                UUID.randomUUID(), ProgrammingLanguage.PYTHON, "print(1)", Instant.now());

        listener.onEvaluationExhausted(event);

        verify(markSubmissionSystemErrorUseCase).execute(eq(submissionId), any(String.class));
    }

    @Test
    void shouldMarkSystemErrorWhenJudgingNotificationRetriesAreExhausted() {
        UUID submissionId = UUID.randomUUID();
        SubmissionJudgingStartedEvent event = new SubmissionJudgingStartedEvent(submissionId, Instant.now());

        listener.onJudgingExhausted(event);

        verify(markSubmissionSystemErrorUseCase).execute(eq(submissionId), any(String.class));
    }

    @Test
    void shouldMarkSystemErrorWhenVerdictRetriesAreExhausted() {
        UUID submissionId = UUID.randomUUID();
        SubmissionJudgedEvent event = new SubmissionJudgedEvent(submissionId, VerdictStatus.ACCEPTED, 10, 100,
                Instant.now());

        listener.onVerdictExhausted(event);

        verify(markSubmissionSystemErrorUseCase).execute(eq(submissionId), any(String.class));
    }

    /** Reencolar un mensaje ya fallido en su propia cola de fallidos solo produciría un bucle. */
    @Test
    void shouldSwallowFailuresWhileMarkingSystemError() {
        UUID submissionId = UUID.randomUUID();
        SubmissionJudgingStartedEvent event = new SubmissionJudgingStartedEvent(submissionId, Instant.now());
        when(markSubmissionSystemErrorUseCase.execute(eq(submissionId), any(String.class)))
                .thenThrow(new RuntimeException("la base de datos no responde"));

        assertThatCode(() -> listener.onJudgingExhausted(event)).doesNotThrowAnyException();
    }

    @Test
    void shouldSwallowSubmissionNotFoundWhileMarkingSystemError() {
        UUID submissionId = UUID.randomUUID();
        SubmissionJudgingStartedEvent event = new SubmissionJudgingStartedEvent(submissionId, Instant.now());
        doThrow(new SubmissionNotFoundException(submissionId))
                .when(markSubmissionSystemErrorUseCase).execute(eq(submissionId), any(String.class));

        assertThatCode(() -> listener.onJudgingExhausted(event)).doesNotThrowAnyException();
    }
}
