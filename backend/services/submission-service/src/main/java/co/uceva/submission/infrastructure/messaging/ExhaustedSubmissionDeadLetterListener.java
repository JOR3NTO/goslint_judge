package co.uceva.submission.infrastructure.messaging;

import co.uceva.shared.domain.event.SubmissionJudgedEvent;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.submission.application.usecase.MarkSubmissionSystemErrorUseCase;
import co.uceva.submission.domain.exception.SubmissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Rescata los envíos que acabaron en una cola de mensajes muertos y los cierra
 * con un estado de error del sistema.
 * <p>
 * Un mensaje llega a esas colas cuando su consumidor lo rechazó definitivamente
 * tras agotar los reintentos. Sin este listener, esos envíos quedarían retenidos
 * en el broker y el estudiante seguiría viendo indefinidamente un envío "en cola"
 * que nadie va a evaluar. Convertirlos en {@code SYSTEM_ERROR} es lo que cierra
 * la espera y permite avisarle.
 * </p>
 * <p>
 * Se vigilan las dos colas de fallidos, porque la evaluación puede romperse en
 * cualquiera de sus dos tramos:
 * </p>
 * <ul>
 *   <li>{@code submission.evaluate.dlq} — el juez no consiguió evaluar el envío.</li>
 *   <li>{@code submission.judged.dlq} — el veredicto llegó pero no pudo registrarse.</li>
 * </ul>
 * <p>
 * Ningún fallo se propaga: si el marcado no sale adelante, volver a encolar el
 * mensaje en su propia cola de fallidos solo produciría un bucle.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExhaustedSubmissionDeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(ExhaustedSubmissionDeadLetterListener.class);

    private final MarkSubmissionSystemErrorUseCase markSubmissionSystemErrorUseCase;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param markSubmissionSystemErrorUseCase Caso de uso que marca el envío como fallido.
     */
    public ExhaustedSubmissionDeadLetterListener(
            MarkSubmissionSystemErrorUseCase markSubmissionSystemErrorUseCase) {
        this.markSubmissionSystemErrorUseCase = markSubmissionSystemErrorUseCase;
    }

    /**
     * Cierra un envío que el motor de evaluación no consiguió procesar.
     *
     * @param event Envío original que el juez rechazó definitivamente.
     */
    @RabbitListener(queues = "${app.messaging.submission.dead-letter-queue}")
    public void onEvaluationExhausted(SubmissionReceivedEvent event) {
        markAsSystemError(event.submissionId(),
                "el motor de evaluación agotó los reintentos sin poder evaluar el envío");
    }

    /**
     * Cierra un envío cuyo veredicto no pudo registrarse.
     *
     * @param event Veredicto que no consiguió aplicarse.
     */
    @RabbitListener(queues = "${app.messaging.submission.judged-dead-letter-queue}")
    public void onVerdictExhausted(SubmissionJudgedEvent event) {
        markAsSystemError(event.submissionId(),
                "se agotaron los reintentos de registro del veredicto emitido por el juez");
    }

    /**
     * Marca el envío como fallido, absorbiendo cualquier error.
     *
     * @param submissionId Envío que no pudo completar su evaluación.
     * @param reason       Motivo del fallo, para trazabilidad.
     */
    private void markAsSystemError(UUID submissionId, String reason) {
        try {
            markSubmissionSystemErrorUseCase.execute(submissionId, reason);
        } catch (SubmissionNotFoundException e) {
            log.error("Mensaje fallido descartado: el envío {} no existe.", submissionId, e);
        } catch (RuntimeException e) {
            log.error("No se pudo marcar el envío {} con estado de error del sistema.", submissionId, e);
        }
    }
}
