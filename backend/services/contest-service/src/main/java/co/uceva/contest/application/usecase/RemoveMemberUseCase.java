package co.uceva.contest.application.usecase;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de retiro de un integrante de un equipo.
 */
public interface RemoveMemberUseCase {
    /**
     * Retira a un usuario de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario a retirar.
     */
    void execute(UUID teamId, UUID userId);
}
