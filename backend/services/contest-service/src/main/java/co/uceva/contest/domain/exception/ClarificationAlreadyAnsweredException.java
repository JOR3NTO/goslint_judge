package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta responder una aclaración que ya fue respondida.
 * Es un caso particular de {@link InvalidClarificationException} que se traduce como conflicto.
 */
public class ClarificationAlreadyAnsweredException extends InvalidClarificationException {

    /** Construye la excepción con un mensaje predeterminado. */
    public ClarificationAlreadyAnsweredException() {
        super("La aclaración ya fue respondida.");
    }
}
