package co.uceva.judge.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para avisar a {@code submission-service} de que la
 * evaluación de un envío acaba de comenzar.
 */
public interface SubmissionJudgingNotifier {

    /**
     * Notifica que el envío indicado empezó a evaluarse.
     *
     * @param submissionId Identificador del envío que se comenzó a evaluar.
     */
    void notifyJudgingStarted(UUID submissionId);
}
