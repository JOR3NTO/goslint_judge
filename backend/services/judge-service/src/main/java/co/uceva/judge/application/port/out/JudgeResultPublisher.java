package co.uceva.judge.application.port.out;

import co.uceva.judge.domain.model.JudgeResult;

/**
 * Puerto de salida para publicar el resultado de una evaluación hacia
 * {@code submission-service}.
 */
public interface JudgeResultPublisher {

    /**
     * Publica el resultado de la evaluación de un envío.
     *
     * @param result Resultado a publicar.
     */
    void publish(JudgeResult result);
}
