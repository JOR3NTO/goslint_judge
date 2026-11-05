package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta crear o registrar un equipo
 * que ya existe en el sistema o en el concurso correspondiente.
 */
public class ExistingTeamException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del equipo duplicado.
     *
     * @param id Identificador del equipo existente.
     */
    public ExistingTeamException(UUID id) {
        super("El equipo con ID '" + id + "' ya existe.");
    }

    /**
     * Construye la excepción indicando el nombre del equipo y el concurso donde ya existe.
     *
     * @param nameTeam  Nombre del equipo duplicado.
     * @param contestId Identificador del concurso.
     */
    public ExistingTeamException(String nameTeam, UUID contestId) {
        super("Ya existe un equipo con el nombre '" + nameTeam + "' en el concurso '" + contestId + "'.");
    }

    /**
     * Construye la excepción con un mensaje descriptivo personalizado.
     *
     * @param message Mensaje descriptivo del error.
     */
    public ExistingTeamException(String message) {
        super(message);
    }
}

