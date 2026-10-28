package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se accede a una aclaración que no existe.
 */
public class ClarificationNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador de la aclaración no encontrada.
     *
     * @param clarificationId Identificador de la aclaración buscada.
     */
    public ClarificationNotFoundException(UUID clarificationId) {
        super("La aclaración con ID '" + clarificationId + "' no existe.");
    }
}
