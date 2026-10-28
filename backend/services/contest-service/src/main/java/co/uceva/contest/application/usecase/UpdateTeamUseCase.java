package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso de actualización de un equipo.
 */
public interface UpdateTeamUseCase {
    /**
     * Ejecuta la actualización de un equipo existente.
     *
     * @param command Datos necesarios para actualizar el equipo.
     * @return El equipo actualizado.
     */
    Team execute(UpdateTeamCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para actualizar un equipo.
     *
     * @param teamId   Identificador del equipo a actualizar.
     * @param teamName Nuevo nombre del equipo.
     */
    record UpdateTeamCommand(
        UUID teamId,
        String teamName
    ){}
}
