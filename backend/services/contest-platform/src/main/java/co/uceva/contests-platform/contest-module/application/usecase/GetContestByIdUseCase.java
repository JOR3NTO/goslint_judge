package co.uceva.contestsplatform.contestmodule.application.usecase;

import java.util.UUID;

import co.uceva.contestsplatform.contestmodule.domain.model.Contest;

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
