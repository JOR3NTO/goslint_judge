package co.uceva.submission.infrastructure.messaging;

import co.uceva.submission.application.event.SubmissionPersistedEvent;
import co.uceva.submission.application.usecase.EnqueueSubmissionUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara el encolamiento de un envío en cuanto la transacción que lo registró
 * ha hecho commit.
 * <p>
 * Esperar al commit evita el problema clásico de escribir en dos sitios a la vez:
 * si el broker se tocara dentro de la transacción, un rollback posterior dejaría
 * al juez evaluando un envío inexistente, y un fallo del broker tumbaría un envío
 * que el estudiante ya había realizado correctamente.
 * </p>
 */
@Component
public class SubmissionEnqueueListener {

    private final EnqueueSubmissionUseCase enqueueSubmissionUseCase;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param enqueueSubmissionUseCase Caso de uso que entrega el envío al motor de evaluación.
     */
    public SubmissionEnqueueListener(EnqueueSubmissionUseCase enqueueSubmissionUseCase) {
        this.enqueueSubmissionUseCase = enqueueSubmissionUseCase;
    }

    /**
     * Encola el envío una vez confirmada su persistencia.
     *
     * @param event Evento interno emitido tras registrar el envío.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionPersisted(SubmissionPersistedEvent event) {
        enqueueSubmissionUseCase.execute(event.submission());
    }
}
