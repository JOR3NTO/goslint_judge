package co.uceva.contest.application.usecase.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.RemoveMemberUseCase;
import co.uceva.contest.domain.exception.MemberNotFoundException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.MemberRepository;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de retiro de integrantes de un equipo.
 */
@Service
public class RemoveMemberUseCaseImpl implements RemoveMemberUseCase {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository   Puerto de salida para consultar equipos.
     * @param memberRepository Puerto de salida para gestionar integrantes.
     */
    public RemoveMemberUseCaseImpl(TeamRepository teamRepository, MemberRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Ejecuta el retiro de un integrante: valida con el dominio que el usuario
     * pertenezca al equipo y elimina su membresía.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario a retirar.
     * @throws TeamNotFoundException   Si el equipo no existe.
     * @throws MemberNotFoundException Si el usuario no pertenece al equipo.
     */
    @Override
    @Transactional
    public void execute(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));

        team.removeMember(userId);
        memberRepository.deleteByTeamIdAndUserId(teamId, userId);
    }
}
