package co.uceva.contests_platform.submission.infrastructure.client;

import co.uceva.contests_platform.contest.application.usecase.GetContestByIdUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetProblemsByContestUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetTeamByIdUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.exception.TeamNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.submission.application.port.out.ContestDataPort;
import co.uceva.contests_platform.submission.domain.model.ContestSnapshot;
import co.uceva.contests_platform.submission.domain.model.TeamSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de {@link ContestDataPort} que lee del módulo {@code contest} invocando
 * directamente sus casos de uso (mismo proceso, sin HTTP).
 * <p>
 * Un equipo, concurso o conjunto de problemas inexistente es una respuesta válida
 * ("no existe") y se traduce en {@code Optional.empty()} (o lista vacía), de modo que la
 * política de elegibilidad rechace el envío con el motivo correspondiente.
 * </p>
 */
@Component
public class ContestServiceDataAdapter implements ContestDataPort {

    private final GetTeamByIdUseCase getTeamByIdUseCase;
    private final GetContestByIdUseCase getContestByIdUseCase;
    private final GetProblemsByContestUseCase getProblemsByContestUseCase;

    /**
     * @param getTeamByIdUseCase          Caso de uso de consulta de equipos.
     * @param getContestByIdUseCase       Caso de uso de consulta de concursos.
     * @param getProblemsByContestUseCase Caso de uso de problemas de un concurso.
     */
    public ContestServiceDataAdapter(
            GetTeamByIdUseCase getTeamByIdUseCase,
            GetContestByIdUseCase getContestByIdUseCase,
            GetProblemsByContestUseCase getProblemsByContestUseCase
    ) {
        this.getTeamByIdUseCase = getTeamByIdUseCase;
        this.getContestByIdUseCase = getContestByIdUseCase;
        this.getProblemsByContestUseCase = getProblemsByContestUseCase;
    }

    @Override
    public Optional<TeamSnapshot> findTeam(UUID teamId) {
        TeamSnapshot ts;
        try {
            Team team = getTeamByIdUseCase.execute(teamId);
            ts = new TeamSnapshot(
                    team.getId(),
                    team.getContestId(),
                    team.getMemberUserIds()
            );
        }catch (TeamNotFoundException e){
            return Optional.empty();
        }
        return Optional.of(ts);
    }

    @Override
    public Optional<ContestSnapshot> findContest(UUID contestId) {
        ContestSnapshot cs;
        try {
            Contest contest = getContestByIdUseCase.execute(contestId);
            cs = new ContestSnapshot(
                    contest.getId(),
                    contest.getStartTime(),
                    contest.getEndTime()
            );
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.of(cs);
    }

    @Override
    public List<UUID> findContestProblemIds(UUID contestId) {
        List<UUID> problems;
        try {
            problems = getProblemsByContestUseCase.execute(contestId)
                    .stream()
                    .map(ContestProblem::getProblemId).toList();
        }catch (ContestNotFoundException e) {
            return List.of();
        }
        return problems;
    }
}
