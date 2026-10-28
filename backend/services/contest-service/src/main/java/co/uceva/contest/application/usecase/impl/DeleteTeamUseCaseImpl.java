package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.DeleteTeamUseCase;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.repository.MemberRepository;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de eliminación de un equipo.
 */
@Service
public class DeleteTeamUseCaseImpl implements DeleteTeamUseCase {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository   Puerto de salida para gestionar equipos.
     * @param memberRepository Puerto de salida para gestionar integrantes.
     */
    public DeleteTeamUseCaseImpl(TeamRepository teamRepository, MemberRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Ejecuta la eliminación de un equipo verificando previamente su existencia
     * y removiendo antes a todos sus integrantes.
     *
     * @param teamId Identificador del equipo a eliminar.
     * @throws TeamNotFoundException Si el equipo no existe.
     */
    @Override
    @Transactional
    public void execute(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
        memberRepository.deleteAllByTeamId(teamId);
        teamRepository.deleteById(teamId);
    }
}
