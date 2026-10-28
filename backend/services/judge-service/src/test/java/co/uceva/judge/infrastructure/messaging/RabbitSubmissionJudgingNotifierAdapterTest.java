package co.uceva.judge.infrastructure.messaging;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import co.uceva.judge.application.exception.JudgingNotificationException;
import co.uceva.shared.domain.event.SubmissionJudgingStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RabbitSubmissionJudgingNotifierAdapterTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private RabbitSubmissionJudgingNotifierAdapter adapter;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        adapter = new RabbitSubmissionJudgingNotifierAdapter(rabbitTemplate, "submission.exchange",
                "submission.judging", 200);
        submissionId = UUID.randomUUID();
    }

    private void stubBroker(boolean ack, boolean returned) {
        doAnswer(invocation -> {
            MessagePostProcessor postProcessor = invocation.getArgument(3);
            Message message = postProcessor.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(submissionId.toString());
            CorrelationData correlation = invocation.getArgument(4);
            if (returned) {
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "x", "k"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("submission.exchange"), eq("submission.judging"),
                any(SubmissionJudgingStartedEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void shouldNotifyWhenBrokerAcknowledges() {
        stubBroker(true, false);

        adapter.notifyJudgingStarted(submissionId);
    }

    @Test
    void shouldFailWhenBrokerNacks() {
        stubBroker(false, false);

        assertThatThrownBy(() -> adapter.notifyJudgingStarted(submissionId))
                .isInstanceOf(JudgingNotificationException.class);
    }

    @Test
    void shouldFailWhenMessageIsReturned() {
        stubBroker(true, true);

        assertThatThrownBy(() -> adapter.notifyJudgingStarted(submissionId))
                .isInstanceOf(JudgingNotificationException.class)
                .hasMessageContaining("enrutarse");
    }

    @Test
    void shouldFailOnTimeout() {
        assertThatThrownBy(() -> adapter.notifyJudgingStarted(submissionId))
                .isInstanceOf(JudgingNotificationException.class);
    }

    @Test
    void shouldWrapCommunicationErrors() {
        doThrow(new IllegalStateException("caído")).when(rabbitTemplate).convertAndSend(any(String.class),
                any(String.class), any(SubmissionJudgingStartedEvent.class), any(MessagePostProcessor.class),
                any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.notifyJudgingStarted(submissionId))
                .isInstanceOf(JudgingNotificationException.class);
    }
}
