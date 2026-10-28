package co.uceva.contest.domain.exception;

/**
 * Excepción lanzada cuando no es posible consultar el catálogo de problemas
 * (servicio caído, tiempo de espera agotado o respuesta inesperada).
 */
public class ProblemCatalogUnavailableException extends RuntimeException {

    /**
     * Construye la excepción con la causa técnica del fallo.
     *
     * @param message Descripción del fallo.
     * @param cause   Causa original.
     */
    public ProblemCatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
