package co.uceva.auth.domain.exception;

/**
 * Excepción de dominio arrojada cuando se intenta registrar un usuario
 * cuyo correo electrónico o nombre de usuario ya existe en el sistema.
 * Es una excepción de negocio que previene la duplicidad de datos.
 */
public class UserAlreadyExistsException extends RuntimeException {
    /**
     * Constructor de la excepción.
     * @param message Mensaje detallado indicando qué dato está duplicado.
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
