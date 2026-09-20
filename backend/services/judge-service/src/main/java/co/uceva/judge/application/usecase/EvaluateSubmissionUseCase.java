package co.uceva.judge.application.usecase;

import co.uceva.judge.domain.model.JudgeResult;
import co.uceva.shared.domain.event.SubmissionReceivedEvent;

/**
 * Puerto de entrada para el caso de uso de evaluación de un envío: orquesta
 * todo el pipeline desde que llega el envío hasta que se publica su veredicto.
 */
public interface EvaluateSubmissionUseCase {

    /**
     * Evalúa el envío recibido y publica el resultado.
     *
     * @param event Evento con el envío a evaluar.
     * @return El resultado de la evaluación.
     */
    JudgeResult execute(SubmissionReceivedEvent event);
}
