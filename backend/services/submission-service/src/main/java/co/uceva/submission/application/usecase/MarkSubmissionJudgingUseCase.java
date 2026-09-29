package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para reflejar que {@code judge-service} tomó un envío y
 * empezó a evaluarlo.
 * <p>
 * Sin este caso de uso, un envío se queda aparentando estar «en cola»
 * (`QUEUED`) durante todo el tiempo que dura la evaluación, en lugar de
 * mostrar que el juez ya lo está procesando.
 * </p>
 */
public interface MarkSubmissionJudgingUseCase {

    /**
     * Marca el envío como en proceso de evaluación y señala el cambio para que
     * se notifique.
     *
     * @param submissionId Identificador del envío que el juez empezó a evaluar.
     * @return El envío ya actualizado y persistido.
     * @throws co.uceva.submission.domain.exception.SubmissionNotFoundException Si el envío no existe.
     */
    Submission execute(UUID submissionId);
}
