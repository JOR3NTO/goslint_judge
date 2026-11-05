package co.uceva.submission.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta acceder a un envío
 * que no existe en el sistema.
 */
public class SubmissionNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del envío no encontrado.
     *
     * @param submissionId Identificador del envío buscado.
     */
    public SubmissionNotFoundException(UUID submissionId) {
        super("El envío con ID '" + submissionId + "' no existe.");
    }
}
