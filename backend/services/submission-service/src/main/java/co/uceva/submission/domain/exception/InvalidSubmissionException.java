package co.uceva.submission.domain.exception;

/**
 * Excepción de dominio lanzada cuando los datos de un envío no cumplen
 * con las reglas de negocio del sistema (e.g. código vacío, lenguaje no
 * soportado o tamaño de código excedido).
 */
public class InvalidSubmissionException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo del error.
     *
     * @param message Detalle de la regla de negocio violada.
     */
    public InvalidSubmissionException(String message) {
        super(message);
    }
}
