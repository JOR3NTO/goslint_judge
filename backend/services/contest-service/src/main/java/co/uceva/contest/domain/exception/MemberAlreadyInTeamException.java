package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando un usuario ya está registrado como integrante
 * en un equipo del mismo concurso o en el mismo equipo.
 */
public class MemberAlreadyInTeamException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del usuario duplicado en el equipo.
     *
     * @param userId Identificador del usuario.
     */
    public MemberAlreadyInTeamException(UUID userId) {
        super("El usuario con ID '" + userId + "' ya pertenece al equipo.");
    }

    /**
     * Construye la excepción con el identificador del usuario y el concurso.
     *
     * @param userId    Identificador del usuario.
     * @param contestId Identificador del concurso.
     */
    public MemberAlreadyInTeamException(UUID userId, UUID contestId) {
        super("El usuario con ID '" + userId + "' ya está inscrito en un equipo para el concurso '" + contestId + "'.");
    }
}

