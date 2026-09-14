package co.uceva.submission.application.port.out;

import co.uceva.submission.domain.model.Submission;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para empujar a los interesados el nuevo estado de un envío.
 * <p>
 * Define un canal de <em>salida y nada más</em>: notifica un cambio ya
 * persistido, sin devolver nada y sin aceptar órdenes de vuelta. La tecnología
 * concreta (hoy WebSocket) queda en la capa de infraestructura, de modo que el
 * flujo de evaluación no depende de ella.
 * </p>
 */
public interface SubmissionStatusNotifier {

    /**
     * Envía el estado actual del envío a los usuarios indicados que tengan una
     * conexión activa.
     * <p>
     * La entrega es de mejor esfuerzo: los destinatarios que no estén conectados
     * simplemente no reciben nada, y consultarán el estado por HTTP la próxima
     * vez que abran la pantalla. Un fallo de entrega no se propaga, porque el
     * veredicto ya está registrado y la notificación solo adelanta lo que la
     * base de datos ya sabe.
     * </p>
     *
     * @param submission       Envío cuyo estado se comunica.
     * @param recipientUserIds Usuarios con derecho a recibir esta actualización.
     */
    void notifyStatusChanged(Submission submission, List<UUID> recipientUserIds);
}
