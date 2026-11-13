package co.uceva.contests_platform.contest.application.usecase;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.model.Contest;

/**
 * Puerto de entrada para el caso de uso de consulta de un concurso por identificador.
 */
public interface GetContestByIdUseCase {
    /**
     * Busca un concurso por su identificador.
     *
     * @param contestId Identificador del concurso.
     * @return El concurso encontrado.
     */
    Contest execute(UUID contestId);
}
