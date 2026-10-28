package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.AddMemberUseCase;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Member;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;

/**
 * Servicio de Aplicación que implementa el caso de uso de incorporación de integrantes a un equipo.
 */
@Service
public class AddMemberUseCaseImpl implements AddMemberUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para gestionar equipos.
     */
    public AddMemberUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Ejecuta la incorporación de un integrante:
     * 1. Busca el equipo.
     * 2. Verifica que el usuario no pertenezca ya a otro equipo del mismo concurso.
     * 3. Agrega el integrante (el dominio valida duplicados y el límite) y persiste el equipo.
     *
     * @param command Datos de entrada para agregar el integrante.
     * @return El integrante agregado.
     * @throws TeamNotFoundException            Si el equipo no existe.
     * @throws MemberAlreadyInTeamException     Si el usuario ya está inscrito en un equipo del concurso.
     * @throws MaxTeamMembersReachedException   Si el equipo alcanzó el máximo de integrantes.
     */
    @Override
    @Transactional
    public Member execute(AddMemberCommand command) {
        Team team = teamRepository.findById(command.teamId())
                .orElseThrow(() -> new TeamNotFoundException(command.teamId()));

        if (teamRepository.existsByContestIdAndUserId(team.getContestId(), command.userId())) {
            throw new MemberAlreadyInTeamException(command.userId(), team.getContestId());
        }

        Member member = team.addMember(command.userId());
        teamRepository.save(team);
        return member;
    }
}
