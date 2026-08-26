package co.uceva.submission.application.usecase;

import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de actualización del veredicto de un
 * envío, ejecutado cuando el motor de evaluación termina su trabajo.
 * <p>
 * Es el punto donde el envío deja de estar en curso: se registra el veredicto
 * junto a las métricas medidas y se señala el cambio para que llegue a la
 * pantalla del estudiante.
 * </p>
 */
public interface UpdateSubmissionVerdictUseCase {

    /**
     * Registra el resultado de la evaluación de un envío.
     *
     * @param command Resultado emitido por el motor de evaluación.
     * @return El envío ya actualizado y persistido.
     * @throws co.uceva.submission.domain.exception.SubmissionNotFoundException Si el envío no existe.
     */
    Submission execute(UpdateSubmissionVerdictCommand command);

    /**
     * Datos con los que el motor de evaluación cierra un envío.
     *
     * @param submissionId    Identificador del envío evaluado.
     * @param verdict         Veredicto emitido.
     * @param executionTimeMs Tiempo de ejecución medido en milisegundos.
     * @param memoryUsedKb    Memoria utilizada medida en kilobytes.
     */
    record UpdateSubmissionVerdictCommand(
            UUID submissionId,
            VerdictStatus verdict,
            int executionTimeMs,
            int memoryUsedKb
    ) {}
}
