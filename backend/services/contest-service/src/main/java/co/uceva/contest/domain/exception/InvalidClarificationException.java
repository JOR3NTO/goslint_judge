package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se crea o responde una aclaración
 * con atributos que violan las invariantes y reglas de negocio del dominio.
 */
public class InvalidClarificationException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo del fallo de validación.
     *
     * @param message Causa por la cual la aclaración es inválida.
     */
    public InvalidClarificationException(String message) {
        super(message);
    }
}
