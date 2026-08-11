package co.uceva.submission.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando un equipo intenta enviar un código
 * fuente idéntico para el mismo problema y en el mismo lenguaje de programación.
 * <p>
 * Esta regla de negocio evita la acumulación de envíos duplicados y
 * contribuye a mantener la integridad del historial de envíos.
 * </p>
 */
public class DuplicateSubmissionException extends RuntimeException {

    /**
     * Construye la excepción con los identificadores del envío duplicado.
     *
     * @param teamId    Identificador del equipo que realizó el envío duplicado.
     * @param problemId Identificador del problema asociado al envío duplicado.
     */
    public DuplicateSubmissionException(UUID teamId, UUID problemId) {
        super("Ya existe un envío idéntico para el problema con ID '" + problemId
                + "' realizado por el equipo con ID '" + teamId + "'.");
    }
}
