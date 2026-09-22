package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

/**
 * Puerto de entrada para el caso de uso de encolamiento de un envío hacia el
 * motor de evaluación.
 * <p>
 * Es el único punto donde vive la lógica de entrega, compartido por el flujo
 * normal (justo después de registrar el envío) y por el reintento automático de
 * los envíos que quedaron rezagados.
 * </p>
 */
public interface EnqueueSubmissionUseCase {

    /**
     * Intenta entregar el envío al motor de evaluación y, si la entrega se
     * confirma, lo marca como encolado.
     * <p>
     * La operación nunca propaga fallos de mensajería: si el motor no está
     * disponible, el envío simplemente permanece en espera y será reintentado
     * más adelante.
     * </p>
     *
     * @param submission Envío a encolar.
     */
    void execute(Submission submission);
}
