package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.UpdateTeamUseCase;
import co.uceva.contest.domain.exception.ExistingTeamException;
import co.uceva.contest.domain.exception.TeamNotFoundException;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.domain.valueobject.TeamName;

/**
 * Servicio de Aplicación que implementa el caso de uso de actualización de un equipo.
 */
@Service
public class UpdateTeamUseCaseImpl implements UpdateTeamUseCase {

    private final TeamRepository teamRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository Puerto de salida para gestionar equipos.
     */
    public UpdateTeamUseCaseImpl(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Ejecuta la actualización de un equipo:
     * 1. Busca el equipo existente.
     * 2. Si el nombre cambia, verifica que no esté en uso dentro del concurso.
     * 3. Actualiza la entidad de dominio y persiste los cambios.
     *
     * @param command Datos de entrada para actualizar el equipo.
     * @return El equipo actualizado y persistido.
     * @throws TeamNotFoundException   Si el equipo no existe.
     * @throws ExistingTeamException   Si el nuevo nombre ya está en uso en el concurso.
     */
    @Override
    @Transactional
    public Team execute(UpdateTeamCommand command) {
        Team team = teamRepository.findById(command.teamId())
                .orElseThrow(() -> new TeamNotFoundException(command.teamId()));

        TeamName newName = TeamName.of(command.teamName());

        if (!newName.equals(team.getTeamName())
                && teamRepository.existsByContestIdAndNameTeam(team.getContestId(), newName.value())) {
            throw new ExistingTeamException(newName.value(), team.getContestId());
        }

        team.updateName(newName);
        return teamRepository.save(team);
    }
}
