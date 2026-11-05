package co.uceva.contest.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.contest.domain.model.Member;

/**
 * Puerto de entrada para el caso de uso: consultar los integrantes de un equipo.
 */
public interface GetMembersByTeamUseCase {
    /**
     * Consultar los integrantes de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @return Lista de resultados.
     */
    List<Member> execute(UUID teamId);
}
