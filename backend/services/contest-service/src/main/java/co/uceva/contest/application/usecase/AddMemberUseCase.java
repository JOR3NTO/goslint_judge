package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Member;

/**
 * Puerto de entrada para el caso de uso de incorporación de un integrante a un equipo.
 */
public interface AddMemberUseCase {
    /**
     * Agrega un usuario como integrante de un equipo.
     *
     * @param command Datos necesarios para agregar el integrante.
     * @return El integrante agregado.
     */
    Member execute(AddMemberCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para agregar un integrante.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario a incorporar.
     */
    record AddMemberCommand(
        UUID teamId,
        UUID userId
    ){}
}
