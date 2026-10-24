package co.uceva.contest.application.usecase.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contest.application.usecase.CreateTeamUseCase;
import co.uceva.contest.domain.exception.ContestNotFoundException;
import co.uceva.contest.domain.exception.ExistingTeamException;
import co.uceva.contest.domain.exception.InvalidContestPasswordException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.model.Team;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.domain.repository.TeamRepository;
import co.uceva.contest.domain.service.PasswordEncoder;
import co.uceva.contest.domain.valueobject.TeamName;

/**
 * Servicio de Aplicación que implementa el caso de uso de creación de equipos.
 * Orquesta la validación de la inscripción y la persistencia del equipo.
 */
@Service
public class CreateTeamUseCaseImpl implements CreateTeamUseCase {

    private final TeamRepository teamRepository;
    private final ContestRepository contestRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository    Puerto de salida para persistir equipos.
     * @param contestRepository Puerto de salida para consultar concursos.
     * @param passwordEncoder   Servicio de dominio para validar la contraseña del concurso.
     */
    public CreateTeamUseCaseImpl(TeamRepository teamRepository, ContestRepository contestRepository,
                                 PasswordEncoder passwordEncoder) {
        this.teamRepository = teamRepository;
        this.contestRepository = contestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ejecuta el flujo de creación de un equipo:
     * 1. Verifica que el concurso exista y que la contraseña de inscripción sea correcta.
     * 2. Verifica que el nombre no esté en uso dentro del concurso.
     * 3. Verifica que el creador no pertenezca ya a otro equipo del concurso.
     * 4. Construye el equipo con el creador como primer integrante y lo persiste.
     *
     * @param command Datos de entrada para crear el equipo.
     * @return El equipo creado y persistido.
     * @throws ContestNotFoundException         Si el concurso no existe.
     * @throws InvalidContestPasswordException  Si la contraseña de inscripción es incorrecta.
     * @throws ExistingTeamException            Si el nombre ya está en uso en el concurso.
     * @throws MemberAlreadyInTeamException     Si el creador ya está inscrito en un equipo del concurso.
     */
    @Override
    @Transactional
    public Team execute(CreateTeamCommand command) {
        Contest contest = contestRepository.findById(command.contestId())
                .orElseThrow(() -> new ContestNotFoundException(command.contestId()));

        contest.assertRegistrationOpen();

        if (command.contestPassword() == null
                || !passwordEncoder.matches(command.contestPassword(), contest.getPasswordHash())) {
            throw new InvalidContestPasswordException(command.contestId());
        }

        TeamName teamName = TeamName.of(command.teamName());

        if (teamRepository.existsByContestIdAndNameTeam(command.contestId(), teamName.value())) {
            throw new ExistingTeamException(teamName.value(), command.contestId());
        }
        if (teamRepository.existsByContestIdAndUserId(command.contestId(), command.creatorUserId())) {
            throw new MemberAlreadyInTeamException(command.creatorUserId(), command.contestId());
        }

        Team team = Team.create(command.contestId(), teamName, command.creatorUserId());
        return teamRepository.save(team);
    }
}
