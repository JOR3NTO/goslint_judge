package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para marcar un envío como fallido por un problema del
 * sistema, una vez agotados todos los reintentos de evaluación.
 * <p>
 * Es la salida de emergencia del ciclo de vida del envío: sin ella, un envío que
 * el motor de evaluación nunca consigue procesar se quedaría indefinidamente
 * aparentando estar en cola, y el estudiante esperando un veredicto que no va a
 * llegar.
 * </p>
 */
public interface MarkSubmissionSystemErrorUseCase {

    /**
     * Marca el envío con un estado de error del sistema y señala el cambio para
     * que se notifique.
     *
     * @param submissionId Identificador del envío que no pudo evaluarse.
     * @param reason       Motivo por el que se descartó, solo para trazabilidad.
     * @return El envío ya actualizado y persistido.
     * @throws co.uceva.submission.domain.exception.SubmissionNotFoundException Si el envío no existe.
     */
    Submission execute(UUID submissionId, String reason);
}
