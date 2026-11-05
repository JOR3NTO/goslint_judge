package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta asignar o actualizar un problema
 * de concurso con atributos que violan las invariantes del dominio.
 */
public class InvalidContestProblemException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo del fallo de validación.
     *
     * @param message Causa por la cual el problema del concurso es inválido.
     */
    public InvalidContestProblemException(String message) {
        super(message);
    }
}
