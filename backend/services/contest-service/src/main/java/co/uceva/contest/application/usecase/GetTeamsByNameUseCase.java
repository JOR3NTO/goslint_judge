package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso de búsqueda de equipos por nombre dentro de un concurso.
 */
public interface GetTeamsByNameUseCase {
    /**
     * Busca los equipos de un concurso cuyo nombre contenga el fragmento dado.
     *
     * @param contestId Identificador del concurso.
     * @param name      Nombre o fragmento a buscar.
     * @return Lista de equipos que coinciden.
     */
    List<Team> execute(UUID contestId, String name);
}
