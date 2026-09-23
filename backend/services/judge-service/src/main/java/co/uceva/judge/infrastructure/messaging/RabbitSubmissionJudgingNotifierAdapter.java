package co.uceva.judge.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.uceva.judge.application.exception.JudgingNotificationException;
import co.uceva.judge.application.port.out.SubmissionJudgingNotifier;
import co.uceva.shared.domain.event.SubmissionJudgingStartedEvent;

/**
 * Adaptador que publica en RabbitMQ el aviso de que un envío empezó a
 * evaluarse, para que {@code submission-service} lo refleje como
 * {@code status = JUDGING}.
 * <p>
 * Igual que {@link RabbitJudgeResultPublisherAdapter}, la publicación es
 * síncrona respecto a la confirmación del broker: si no hay confirmación el
 * método falla, y con él la evaluación, que la mensajería reintenta completa.
 * Tratarlo con la misma exigencia que el veredicto evita que un envío se quede
 * aparentando estar «en cola» mientras el juez ya lo está procesando.
 * </p>
 */
@Component
public class RabbitSubmissionJudgingNotifierAdapter implements SubmissionJudgingNotifier {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final long confirmTimeoutMs;

    /**
     * @param rabbitTemplate   Plantilla de publicación configurada con confirmaciones.
     * @param exchange         Exchange principal de envíos.
     * @param routingKey       Routing key del aviso de inicio de evaluación.
     * @param confirmTimeoutMs Tiempo máximo de espera de la confirmación del broker.
     */
    public RabbitSubmissionJudgingNotifierAdapter(RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.submission.exchange}") String exchange,
            @Value("${app.messaging.submission.judging-routing-key}") String routingKey,
            @Value("${app.messaging.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    /**
     * Publica el aviso como mensaje persistente, con el identificador del envío
     * como {@code messageId}, y espera la confirmación del broker.
     *
     * @param submissionId Identificador del envío que se comenzó a evaluar.
     * @throws JudgingNotificationException Si el broker rechaza el mensaje, no responde a
     *                                      tiempo, lo devuelve por no ser enrutable o falla la
     *                                      comunicación.
     */
    @Override
    public void notifyJudgingStarted(UUID submissionId) {
        SubmissionJudgingStartedEvent event = new SubmissionJudgingStartedEvent(submissionId, Instant.now());
        String messageId = submissionId.toString();
        CorrelationData correlationData = new CorrelationData(messageId);

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                message.getMessageProperties().setMessageId(messageId);
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }, correlationData);

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

            if (confirm == null || !confirm.isAck()) {
                throw new JudgingNotificationException(submissionId,
                        "el broker no acusó recibo (" + (confirm == null ? "sin respuesta" : confirm.getReason()) + ")");
            }

            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new JudgingNotificationException(submissionId,
                        "el mensaje no pudo enrutarse a ninguna cola: " + returned.getReplyText());
            }
        } catch (JudgingNotificationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgingNotificationException(submissionId,
                    "la espera de la confirmación fue interrumpida", e);
        } catch (Exception e) {
            throw new JudgingNotificationException(submissionId,
                    "fallo al comunicarse con el sistema de mensajería", e);
        }
    }
}
