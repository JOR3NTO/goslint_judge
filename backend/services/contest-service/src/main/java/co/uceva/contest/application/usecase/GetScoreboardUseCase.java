package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Team;

/**
 * Puerto de entrada para el caso de uso: consultar la tabla de posiciones de un concurso (mayor puntaje y menor penalización primero).
 */
public interface GetScoreboardUseCase {
    /**
     * Consultar la tabla de posiciones de un concurso (mayor puntaje y menor penalización primero).
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    List<Team> execute(UUID contestId);
}
