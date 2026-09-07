package co.uceva.submission.infrastructure.notification;

import co.uceva.submission.application.event.SubmissionStatusChangedEvent;
import co.uceva.submission.application.usecase.NotifySubmissionStatusUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara la notificación del nuevo estado de un envío en cuanto la transacción
 * que lo registró ha hecho commit.
 * <p>
 * Esperar al commit es lo que hace que la pantalla del estudiante y la base de
 * datos digan siempre lo mismo: si la transacción acabara deshaciéndose, el
 * veredicto notificado ya no existiría en ninguna parte y no habría forma de
 * retirarlo de una pantalla que no se recarga.
 * </p>
 * <p>
 * Un fallo de notificación se registra pero no se propaga: el veredicto ya está
 * persistido y accesible por HTTP, así que no llegar a empujarlo es una molestia,
 * nunca una pérdida de datos.
 * </p>
 */
@Component
public class SubmissionStatusNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionStatusNotificationListener.class);

    private final NotifySubmissionStatusUseCase notifySubmissionStatusUseCase;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param notifySubmissionStatusUseCase Caso de uso que resuelve destinatarios y notifica.
     */
    public SubmissionStatusNotificationListener(NotifySubmissionStatusUseCase notifySubmissionStatusUseCase) {
        this.notifySubmissionStatusUseCase = notifySubmissionStatusUseCase;
    }

    /**
     * Notifica el envío una vez confirmado el cambio de estado.
     *
     * @param event Evento interno emitido tras actualizar el estado del envío.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionStatusChanged(SubmissionStatusChangedEvent event) {
        try {
            notifySubmissionStatusUseCase.execute(event.submission());
        } catch (RuntimeException e) {
            log.warn("No se pudo notificar el nuevo estado del envío {}.", event.submission().getId(), e);
        }
    }
}
