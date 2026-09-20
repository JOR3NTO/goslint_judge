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

import co.uceva.judge.application.exception.ResultPublishingException;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.shared.domain.event.SubmissionJudgedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class RabbitJudgeResultPublisherAdapterTest {

    @Mock private RabbitTemplate rabbitTemplate;
    private RabbitJudgeResultPublisherAdapter adapter;
    private JudgeResult result;

    @BeforeEach
    void setUp() {
        adapter = new RabbitJudgeResultPublisherAdapter(rabbitTemplate, "submission.exchange", "submission.judged", 200);
        result = JudgeResult.create(UUID.randomUUID(), VerdictStatus.ACCEPTED, 10, 100, null);
    }

    private void stubBroker(boolean ack, boolean returned) {
        doAnswer(invocation -> {
            MessagePostProcessor postProcessor = invocation.getArgument(3);
            Message message = postProcessor.postProcessMessage(new Message(new byte[0], new MessageProperties()));
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(result.getSubmissionId().toString());
            CorrelationData correlation = invocation.getArgument(4);
            if (returned) {
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "x", "k"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq("submission.exchange"), eq("submission.judged"),
                any(SubmissionJudgedEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void shouldPublishWhenBrokerAcknowledges() {
        stubBroker(true, false);

        adapter.publish(result);
    }

    @Test
    void shouldFailWhenBrokerNacks() {
        stubBroker(false, false);

        assertThatThrownBy(() -> adapter.publish(result)).isInstanceOf(ResultPublishingException.class);
    }

    @Test
    void shouldFailWhenMessageIsReturned() {
        stubBroker(true, true);

        assertThatThrownBy(() -> adapter.publish(result))
                .isInstanceOf(ResultPublishingException.class)
                .hasMessageContaining("enrutarse");
    }

    @Test
    void shouldFailOnTimeout() {
        assertThatThrownBy(() -> adapter.publish(result)).isInstanceOf(ResultPublishingException.class);
    }

    @Test
    void shouldWrapCommunicationErrors() {
        doThrow(new IllegalStateException("caído")).when(rabbitTemplate).convertAndSend(any(String.class),
                any(String.class), any(SubmissionJudgedEvent.class), any(MessagePostProcessor.class),
                any(CorrelationData.class));

        assertThatThrownBy(() -> adapter.publish(result)).isInstanceOf(ResultPublishingException.class);
    }
}
