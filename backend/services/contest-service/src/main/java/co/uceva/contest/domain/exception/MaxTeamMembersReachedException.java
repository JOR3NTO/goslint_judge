package co.uceva.contest.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta agregar un nuevo integrante
 * a un equipo que ya alcanzó la cantidad máxima permitida.
 */
public class MaxTeamMembersReachedException extends RuntimeException {

    /**
     * Construye la excepción con la cantidad máxima de integrantes permitidos.
     *
     * @param maxMembers Cantidad máxima permitida.
     */
    public MaxTeamMembersReachedException(int maxMembers) {
        super("El equipo ya alcanzó el límite máximo de integrantes permitidos (" + maxMembers + ").");
    }
}

