package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta acceder o realizar una operación
 * sobre un equipo que no existe en el sistema.
 */
public class TeamNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del equipo no encontrado.
     *
     * @param teamId Identificador del equipo buscado.
     */
    public TeamNotFoundException(UUID teamId) {
        super("El equipo con ID '" + teamId + "' no existe.");
    }

    /**
     * Construye la excepción con un mensaje personalizado.
     *
     * @param message Mensaje descriptivo del error.
     */
    public TeamNotFoundException(String message) {
        super(message);
    }
}

