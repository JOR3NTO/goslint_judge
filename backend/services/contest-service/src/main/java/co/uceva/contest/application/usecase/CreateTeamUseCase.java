package co.uceva.contest.application.usecase;

import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso de creación (inscripción) de un equipo en un concurso.
 */
public interface CreateTeamUseCase {
    /**
     * Ejecuta la creación de un nuevo equipo.
     *
     * @param command Datos necesarios para crear el equipo.
     * @return El equipo creado, con su creador como primer integrante.
     */
    Team execute(CreateTeamCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para crear un equipo.
     *
     * @param contestId       Identificador del concurso al que se inscribe el equipo.
     * @param teamName        Nombre del equipo.
     * @param creatorUserId   Identificador del usuario que crea e integra el equipo.
     * @param contestPassword Contraseña de inscripción del concurso en texto plano.
     */
    record CreateTeamCommand(
        UUID contestId,
        String teamName,
        UUID creatorUserId,
        String contestPassword
    ){}
}
