package co.uceva.submission.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación de un envío.
 */
public interface DeleteSubmissionUseCase {

    /**
     * Elimina un envío del sistema.
     *
     * @param submissionId Identificador del envío a eliminar.
     */
    void execute(UUID submissionId);
}
