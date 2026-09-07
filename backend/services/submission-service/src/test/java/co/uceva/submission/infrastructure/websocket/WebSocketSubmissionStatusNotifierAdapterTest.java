package co.uceva.submission.infrastructure.websocket;

import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprueba qué sale por el canal y hacia quién.
 */
class WebSocketSubmissionStatusNotifierAdapterTest {

    private static final UUID DESTINATARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID AJENO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private WebSocketSessionRegistry sessionRegistry;
    private ObjectMapper objectMapper;
    private WebSocketSubmissionStatusNotifierAdapter adapter;

    @BeforeEach
    void setUp() {
        sessionRegistry = new WebSocketSessionRegistry();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new WebSocketSubmissionStatusNotifierAdapter(sessionRegistry, objectMapper);
    }

    /** El mensaje lleva justo lo que la pantalla necesita para actualizarse. */
    @Test
    void shouldPushTheVerdictWithItsExecutionTimeAndMemoryUsed() throws Exception {
        WebSocketSession session = openSession();
        sessionRegistry.register(DESTINATARIO, session);
        Submission submission = judgedSubmission();

        adapter.notifyStatusChanged(submission, List.of(DESTINATARIO));

        JsonNode payload = objectMapper.readTree(captureMessage(session).getPayload());
        assertThat(payload.get("type").asText()).isEqualTo("SUBMISSION_STATUS_UPDATED");
        assertThat(payload.get("submissionId").asText()).isEqualTo(submission.getId().toString());
        assertThat(payload.get("status").asText()).isEqualTo("JUDGED");
        assertThat(payload.get("verdict").asText()).isEqualTo("ACCEPTED");
        assertThat(payload.get("executionTimeMs").asInt()).isEqualTo(120);
        assertThat(payload.get("memoryUsedKb").asInt()).isEqualTo(2048);
    }

    /**
     * El código fuente ya está en poder de quien lo envió; no tiene por qué recorrer
     * la red otra vez ni aparecer en los registros de nadie.
     */
    @Test
    void shouldNotLeakTheSourceCodeThroughTheChannel() throws Exception {
        WebSocketSession session = openSession();
        sessionRegistry.register(DESTINATARIO, session);

        adapter.notifyStatusChanged(judgedSubmission(), List.of(DESTINATARIO));

        assertThat(captureMessage(session).getPayload())
                .doesNotContain(SubmissionFixtures.SOURCE_CODE)
                .doesNotContain("sourceCode");
    }

    /** Nadie recibe envíos que no le pertenecen, ni siquiera estando conectado. */
    @Test
    void shouldNeverReachAUserOutsideTheRecipientList() throws Exception {
        WebSocketSession sesionAjena = openSession();
        sessionRegistry.register(AJENO, sesionAjena);

        adapter.notifyStatusChanged(judgedSubmission(), List.of(DESTINATARIO));

        verify(sesionAjena, never()).sendMessage(any());
    }

    /** Varias pestañas del mismo usuario se actualizan todas a la vez. */
    @Test
    void shouldReachEveryOpenSessionOfTheRecipient() throws Exception {
        WebSocketSession primeraPestana = openSession();
        WebSocketSession segundaPestana = openSession();
        sessionRegistry.register(DESTINATARIO, primeraPestana);
        sessionRegistry.register(DESTINATARIO, segundaPestana);

        adapter.notifyStatusChanged(judgedSubmission(), List.of(DESTINATARIO));

        verify(primeraPestana).sendMessage(any(TextMessage.class));
        verify(segundaPestana).sendMessage(any(TextMessage.class));
    }

    /** Una conexión que ya se cerró no rompe la entrega a las demás. */
    @Test
    void shouldSkipClosedSessions() throws Exception {
        WebSocketSession cerrada = mock(WebSocketSession.class);
        when(cerrada.isOpen()).thenReturn(false);
        sessionRegistry.register(DESTINATARIO, cerrada);

        adapter.notifyStatusChanged(judgedSubmission(), List.of(DESTINATARIO));

        verify(cerrada, never()).sendMessage(any());
    }

    /**
     * El veredicto ya está persistido: no poder empujarlo es una molestia, nunca un
     * motivo para deshacer nada.
     */
    @Test
    void shouldNotPropagateDeliveryFailures() throws Exception {
        WebSocketSession rota = openSession();
        doThrow(new IOException("conexión rota")).when(rota).sendMessage(any());
        WebSocketSession sana = openSession();
        sessionRegistry.register(DESTINATARIO, rota);
        sessionRegistry.register(DESTINATARIO, sana);

        assertThatCode(() -> adapter.notifyStatusChanged(judgedSubmission(), List.of(DESTINATARIO)))
                .doesNotThrowAnyException();
        verify(sana).sendMessage(any(TextMessage.class));
    }

    private Submission judgedSubmission() {
        Submission submission = SubmissionFixtures.aSubmission(
                SubmissionFixtures.SUBMISSION_ID, SubmissionStatus.QUEUED);
        submission.updateVerdict(VerdictStatus.ACCEPTED, 120, 2048);
        return submission;
    }

    private WebSocketSession openSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private TextMessage captureMessage(WebSocketSession session) throws IOException {
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return captor.getValue();
    }
}
