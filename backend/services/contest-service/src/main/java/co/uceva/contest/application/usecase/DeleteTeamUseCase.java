package co.uceva.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de eliminación de un equipo.
 */
public interface DeleteTeamUseCase {
    /**
     * Elimina un equipo junto con sus integrantes.
     *
     * @param teamId Identificador del equipo a eliminar.
     */
    void execute(UUID teamId);
}
