package co.uceva.contest.domain.exception;

import java.util.UUID;

/**
 * Excepción de dominio lanzada cuando se intenta acceder o eliminar un integrante
 * que no pertenece al equipo especificado.
 */
public class MemberNotFoundException extends RuntimeException {

    /**
     * Construye la excepción con el identificador del usuario buscado.
     *
     * @param userId Identificador del usuario no encontrado en el equipo.
     */
    public MemberNotFoundException(UUID userId) {
        super("El integrante con ID '" + userId + "' no pertenece al equipo.");
    }

    /**
     * Construye la excepción con el identificador del usuario y del equipo.
     *
     * @param userId Identificador del usuario.
     * @param teamId Identificador del equipo.
     */
    public MemberNotFoundException(UUID userId, UUID teamId) {
        super("El integrante con ID '" + userId + "' no pertenece al equipo '" + teamId + "'.");
    }
}

