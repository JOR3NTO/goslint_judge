package co.uceva.judge.infrastructure.messaging;

import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.uceva.judge.application.exception.ResultPublishingException;
import co.uceva.judge.application.port.out.JudgeResultPublisher;
import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.judge.infrastructure.mapper.JudgeResultEventMapper;
import co.uceva.shared.domain.event.SubmissionJudgedEvent;

/**
 * Adaptador que publica el resultado de la evaluación en RabbitMQ para que
 * {@code submission-service} registre el veredicto.
 * <p>
 * La publicación es síncrona respecto a la confirmación del broker: si no hay
 * confirmación el método falla, y con él la evaluación, que la mensajería
 * reintenta. Así un veredicto nunca se da por entregado sin estarlo.
 * </p>
 */
@Component
public class RabbitJudgeResultPublisherAdapter implements JudgeResultPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final long confirmTimeoutMs;

    /**
     * @param rabbitTemplate   Plantilla de publicación configurada con confirmaciones.
     * @param exchange         Exchange principal de envíos.
     * @param routingKey       Routing key de los veredictos.
     * @param confirmTimeoutMs Tiempo máximo de espera de la confirmación del broker.
     */
    public RabbitJudgeResultPublisherAdapter(RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.submission.exchange}") String exchange,
            @Value("${app.messaging.submission.judged-routing-key}") String routingKey,
            @Value("${app.messaging.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    /**
     * Publica el veredicto como mensaje persistente, con el identificador del
     * envío como {@code messageId}, y espera la confirmación del broker.
     *
     * @param result Resultado de la evaluación.
     * @throws ResultPublishingException Si el broker rechaza el mensaje, no responde a
     *                                   tiempo, lo devuelve por no ser enrutable o falla la comunicación.
     */
    @Override
    public void publish(JudgeResult result) {
        SubmissionJudgedEvent event = JudgeResultEventMapper.toEvent(result);
        String submissionId = result.getSubmissionId().toString();
        CorrelationData correlationData = new CorrelationData(submissionId);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(submissionId);
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }, correlationData);

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

            if (confirm == null || !confirm.isAck()) {
                throw new ResultPublishingException(result.getSubmissionId(),
                        "el broker no acusó recibo (" + (confirm == null ? "sin respuesta" : confirm.getReason()) + ")");
            }

            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new ResultPublishingException(result.getSubmissionId(),
                        "el mensaje no pudo enrutarse a ninguna cola: " + returned.getReplyText());
            }
        } catch (ResultPublishingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResultPublishingException(result.getSubmissionId(),
                    "la espera de la confirmación fue interrumpida", e);
        } catch (Exception e) {
            throw new ResultPublishingException(result.getSubmissionId(),
                    "fallo al comunicarse con el sistema de mensajería", e);
        }
    }
}
