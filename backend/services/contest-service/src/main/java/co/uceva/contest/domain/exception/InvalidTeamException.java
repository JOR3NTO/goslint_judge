package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta crear o actualizar un equipo
 * con atributos que violan las invariantes y reglas de negocio del dominio.
 */
public class InvalidTeamException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje descriptivo del fallo de validación.
     *
     * @param message Causa por la cual el equipo es inválido.
     */
    public InvalidTeamException(String message) {
        super(message);
    }
}

