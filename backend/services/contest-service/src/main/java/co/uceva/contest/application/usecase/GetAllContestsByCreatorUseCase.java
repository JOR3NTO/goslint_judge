package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Contest;

/**
 * Puerto de entrada para el caso de uso de consulta de los concursos de un creador.
 */
public interface GetAllContestsByCreatorUseCase {
    /**
     * Recupera los concursos creados por un usuario.
     *
     * @param createdBy Identificador del usuario creador.
     * @return Lista de concursos creados por dicho usuario.
     */
    List<Contest> execute(UUID createdBy);
}
