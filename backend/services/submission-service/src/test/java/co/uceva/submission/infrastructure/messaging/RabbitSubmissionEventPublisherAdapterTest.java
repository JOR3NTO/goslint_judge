package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.submission.application.exception.EventPublishingException;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.fixtures.SubmissionFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitSubmissionEventPublisherAdapterTest {

    private static final String EXCHANGE = "submission.exchange";
    private static final String ROUTING_KEY = "submission.evaluate";
    private static final long CONFIRM_TIMEOUT_MS = 200L;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitSubmissionEventPublisherAdapter adapter() {
        return new RabbitSubmissionEventPublisherAdapter(
                rabbitTemplate, EXCHANGE, ROUTING_KEY, CONFIRM_TIMEOUT_MS);
    }

    /** Simula la respuesta del broker completando la confirmación pendiente. */
    private void stubBrokerConfirm(boolean ack, String reason, ReturnedMessage returned) {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.setReturned(returned);
            correlationData.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(EXCHANGE), eq(ROUTING_KEY), any(SubmissionReceivedEvent.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void shouldPublishToTheEvaluationQueueWhenTheBrokerAcknowledges() {
        Submission submission = SubmissionFixtures.aSubmission();
        stubBrokerConfirm(true, null, null);

        assertThatCode(() -> adapter().publishSubmissionReceived(submission)).doesNotThrowAnyException();

        ArgumentCaptor<SubmissionReceivedEvent> event = ArgumentCaptor.forClass(SubmissionReceivedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), event.capture(),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThat(event.getValue().submissionId()).isEqualTo(submission.getId());
        assertThat(event.getValue().teamId()).isEqualTo(submission.getTeamId());
        assertThat(event.getValue().problemId()).isEqualTo(submission.getProblemId());
        assertThat(event.getValue().sourceCode()).isEqualTo(submission.getSourceCode());
    }

    @Test
    void shouldSendPersistentMessagesIdentifiedBySubmissionId() {
        Submission submission = SubmissionFixtures.aSubmission();
        stubBrokerConfirm(true, null, null);

        adapter().publishSubmissionReceived(submission);

        ArgumentCaptor<MessagePostProcessor> postProcessor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY),
                any(SubmissionReceivedEvent.class), postProcessor.capture(), any(CorrelationData.class));

        Message message = postProcessor.getValue()
                .postProcessMessage(new Message(new byte[0], new MessageProperties()));

        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(submission.getId().toString());
        assertThat(message.getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    void shouldFailWhenTheBrokerRejectsTheMessage() {
        Submission submission = SubmissionFixtures.aSubmission();
        stubBrokerConfirm(false, "cola inexistente", null);

        assertThatThrownBy(() -> adapter().publishSubmissionReceived(submission))
                .isInstanceOf(EventPublishingException.class)
                .hasMessageContaining(submission.getId().toString());
    }

    @Test
    void shouldFailWhenTheMessageCannotBeRoutedToAnyQueue() {
        Submission submission = SubmissionFixtures.aSubmission();
        ReturnedMessage returned = new ReturnedMessage(
                new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY);
        stubBrokerConfirm(true, null, returned);

        assertThatThrownBy(() -> adapter().publishSubmissionReceived(submission))
                .isInstanceOf(EventPublishingException.class)
                .hasMessageContaining("NO_ROUTE");
    }

    @Test
    void shouldFailWhenTheBrokerDoesNotConfirmInTime() {
        Submission submission = SubmissionFixtures.aSubmission();
        // El broker nunca responde: la confirmación queda pendiente para siempre.

        assertThatThrownBy(() -> adapter().publishSubmissionReceived(submission))
                .isInstanceOf(EventPublishingException.class);
    }

    @Test
    void shouldFailWhenTheBrokerIsUnreachable() {
        Submission submission = SubmissionFixtures.aSubmission();
        doThrow(new AmqpConnectException(new RuntimeException("conexión rechazada")))
                .when(rabbitTemplate).convertAndSend(
                        eq(EXCHANGE), eq(ROUTING_KEY), any(SubmissionReceivedEvent.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> adapter().publishSubmissionReceived(submission))
                .isInstanceOf(EventPublishingException.class);
    }
}
