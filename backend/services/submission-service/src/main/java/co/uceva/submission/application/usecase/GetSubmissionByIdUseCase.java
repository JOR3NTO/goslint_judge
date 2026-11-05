package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de consulta de un envío por identificador.
 */
public interface GetSubmissionByIdUseCase {

    /**
     * Busca un envío por su identificador.
     *
     * @param submissionId Identificador del envío.
     * @return El envío encontrado.
     */
    Submission execute(UUID submissionId);
}
