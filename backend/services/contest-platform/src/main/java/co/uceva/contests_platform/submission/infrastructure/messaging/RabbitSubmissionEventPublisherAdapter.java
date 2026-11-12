package co.uceva.contests_platform.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.contests_platform.submission.application.exception.EventPublishingException;
import co.uceva.contests_platform.submission.application.port.out.ContestDataPort;
import co.uceva.contests_platform.submission.application.port.out.SubmissionEventPublisher;
import co.uceva.contests_platform.submission.domain.model.Submission;
import co.uceva.contests_platform.submission.infrastructure.mapper.SubmissionEventMapper;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de infraestructura que publica los envíos recibidos en RabbitMQ para
 * que {@code judge-service} los evalúe.
 * <p>
 * La publicación es síncrona respecto a la confirmación del broker: el método no
 * retorna hasta que RabbitMQ acusa recibo del mensaje. Es lo que permite marcar
 * un envío como encolado solo cuando la entrega está realmente garantizada, y no
 * meramente intentada.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitSubmissionEventPublisherAdapter implements SubmissionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ContestDataPort contestDataPort;
    private final String exchange;
    private final String routingKey;
    private final long confirmTimeoutMs;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param rabbitTemplate   Plantilla de publicación configurada con confirmaciones.
     * @param exchange         Exchange al que se publican los eventos de envíos.
     * @param routingKey       Routing key de los envíos pendientes de evaluar.
     * @param confirmTimeoutMs Tiempo máximo de espera de la confirmación del broker.
     */
    public RabbitSubmissionEventPublisherAdapter(RabbitTemplate rabbitTemplate, ContestDataPort contestDataPort,
            @Value("${app.messaging.submission.exchange}") String exchange,
            @Value("${app.messaging.submission.routing-key}") String routingKey,
            @Value("${app.messaging.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.contestDataPort = contestDataPort;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    /**
     * Publica el evento de envío recibido y espera la confirmación del broker.
     * <p>
     * El mensaje se marca como persistente para que sobreviva a un reinicio de
     * RabbitMQ, y lleva el identificador del envío como {@code messageId} para
     * que el consumidor pueda descartar entregas duplicadas.
     * </p>
     *
     * @param submission El envío recién creado y persistido.
     * @throws EventPublishingException Si el broker rechaza el mensaje, no responde
     *                                  a tiempo, lo devuelve por no ser enrutable, o
     *                                  la comunicación falla.
     */
    @Override
    public void publishSubmissionReceived(Submission submission) {
        SubmissionReceivedEvent event = SubmissionEventMapper.toEvent(submission, catalogProblemId(submission));
        String submissionId = submission.getId().toString();
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
                throw new EventPublishingException(submission.getId(),
                        "el broker no acusó recibo del mensaje (" +
                                (confirm == null ? "sin respuesta" : confirm.getReason()) + ")");
            }

            // Un mensaje devuelto llegó al broker pero no encajó en ninguna cola:
            // nadie lo evaluaría, así que no cuenta como entregado.
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new EventPublishingException(submission.getId(),
                        "el mensaje no pudo enrutarse a ninguna cola: " + returned.getReplyText());
            }
        } catch (EventPublishingException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException(submission.getId(),
                    "la espera de la confirmación fue interrumpida", e);
        } catch (Exception e) {
            throw new EventPublishingException(submission.getId(),
                    "fallo al comunicarse con el sistema de mensajería", e);
        }
    }

    /** El juez carga límites y casos de prueba desde el catálogo, no desde el problema del concurso. */
    private UUID catalogProblemId(Submission submission) {
        return contestDataPort.findTeam(submission.getTeamId())
                .flatMap(team -> contestDataPort.findCatalogProblemId(team.contestId(), submission.getProblemId()))
                .orElseThrow(() -> new EventPublishingException(submission.getId(),
                        "el problema " + submission.getProblemId() + " no pertenece al concurso del equipo"));
    }
}
