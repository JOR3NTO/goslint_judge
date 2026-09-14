package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionJudgedEvent;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase;
import co.uceva.submission.application.usecase.UpdateSubmissionVerdictUseCase.UpdateSubmissionVerdictCommand;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consume los veredictos que {@code judge-service} publica al terminar de evaluar
 * un envío.
 * <p>
 * Es el punto de entrada del cierre del ciclo de vida del envío. Su único trabajo
 * es traducir el mensaje a un comando y delegarlo: aquí no se decide nada sobre
 * el veredicto ni sobre a quién notificarlo.
 * </p>
 * <p>
 * Un fallo transitorio (la base de datos no responde) se propaga para que el
 * reintento del contenedor de escucha vuelva a intentarlo, y si se agotan los
 * reintentos el mensaje acaba en la cola de fallidos. Un mensaje que nunca podrá
 * procesarse, en cambio, se rechaza de inmediato sin gastar reintentos.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SubmissionJudgedListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionJudgedListener.class);

    private final UpdateSubmissionVerdictUseCase updateSubmissionVerdictUseCase;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param updateSubmissionVerdictUseCase Caso de uso que registra el veredicto del envío.
     */
    public SubmissionJudgedListener(UpdateSubmissionVerdictUseCase updateSubmissionVerdictUseCase) {
        this.updateSubmissionVerdictUseCase = updateSubmissionVerdictUseCase;
    }

    /**
     * Registra el veredicto recibido del motor de evaluación.
     *
     * @param event Resultado de la evaluación publicado por {@code judge-service}.
     * @throws AmqpRejectAndDontRequeueException Si el envío referido no existe.
     */
    @RabbitListener(queues = "${app.messaging.submission.judged-queue}")
    public void onSubmissionJudged(SubmissionJudgedEvent event) {
        try {
            updateSubmissionVerdictUseCase.execute(new UpdateSubmissionVerdictCommand(
                    event.submissionId(),
                    event.verdict(),
                    event.executionTimeMs(),
                    event.memoryUsedKb()));
        } catch (SubmissionNotFoundException e) {
            // Reintentar no lo haría aparecer: el mensaje va directo a la cola de
            // fallidos en lugar de dar vueltas hasta agotar los intentos.
            log.error("Veredicto descartado: el envío {} no existe.", event.submissionId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
