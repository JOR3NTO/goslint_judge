package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionJudgingStartedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionJudgingUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consume el aviso de inicio de evaluación que {@code judge-service} publica
 * al tomar un envío.
 * <p>
 * Su único trabajo es traducir el mensaje a un comando y delegarlo, igual que
 * {@link SubmissionJudgedListener}: aquí no se decide nada sobre el estado del
 * envío.
 * </p>
 * <p>
 * Un fallo transitorio (la base de datos no responde) se propaga para que el
 * reintento del contenedor de escucha vuelva a intentarlo, y si se agotan los
 * reintentos el mensaje acaba en la cola de fallidos. Un mensaje que nunca
 * podrá procesarse, en cambio, se rechaza de inmediato sin gastar reintentos.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionJudgingListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionJudgingListener.class);

    private final MarkSubmissionJudgingUseCase markSubmissionJudgingUseCase;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param markSubmissionJudgingUseCase Caso de uso que marca el envío como en evaluación.
     */
    public SubmissionJudgingListener(MarkSubmissionJudgingUseCase markSubmissionJudgingUseCase) {
        this.markSubmissionJudgingUseCase = markSubmissionJudgingUseCase;
    }

    /**
     * Registra el inicio de la evaluación del envío recibido.
     *
     * @param event Aviso publicado por {@code judge-service}.
     * @throws AmqpRejectAndDontRequeueException Si el envío referido no existe.
     */
    @RabbitListener(queues = "${app.messaging.submission.judging-queue}")
    public void onSubmissionJudgingStarted(SubmissionJudgingStartedEvent event) {
        try {
            markSubmissionJudgingUseCase.execute(event.submissionId());
        } catch (SubmissionNotFoundException e) {
            // Reintentar no lo haría aparecer: el mensaje va directo a la cola de
            // fallidos en lugar de dar vueltas hasta agotar los intentos.
            log.error("Aviso de inicio de evaluación descartado: el envío {} no existe.", event.submissionId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
