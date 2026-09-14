package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de consulta de todos los envíos
 * asociados a un problema específico.
 */
public interface GetSubmissionsByProblemUseCase {

    /**
     * Recupera todos los envíos de un problema.
     *
     * @param problemId Identificador del problema.
     * @return Lista de envíos asociados al problema.
     */
    List<Submission> execute(UUID problemId);
}
