package co.uceva.contests_platform.submission.infrastructure.client;

import co.uceva.contests_platform.contest.application.usecase.GetContestByIdUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetProblemsByContestUseCase;
import co.uceva.contests_platform.contest.application.usecase.GetTeamByIdUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.exception.TeamNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.submission.application.exception.ContestServiceUnavailableException;
import co.uceva.contests_platform.submission.application.port.out.ContestDataPort;
import co.uceva.contests_platform.submission.domain.model.ContestSnapshot;
import co.uceva.contests_platform.submission.domain.model.TeamSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de {@link ContestDataPort} que consulta los endpoints públicos de
 * {@code contest-service} mediante {@link RestClient}.
 * <p>
 * Distingue dos clases de resultado: un {@code 404} es una respuesta válida
 * ("no existe") y se traduce en {@code Optional.empty()} (o lista vacía); cualquier
 * otro fallo, ya sea un estado de error o un problema de red, se traduce en
 * {@link ContestServiceUnavailableException}, porque en ese caso no se sabe si el
 * recurso existe y rechazar el envío como si no existiera sería engañoso.
 * </p>
 */
@Component
public class ContestServiceDataAdapter implements ContestDataPort {

    private final GetTeamByIdUseCase getTeamByIdUseCase;
    private final GetContestByIdUseCase getContestByIdUseCase;
    private final GetProblemsByContestUseCase getProblemsByContestUseCase;

    /** @param restClient Cliente HTTP configurado contra {@code contest-service}. */
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
                    .map(ContestProblem::getId).toList();
        }catch (ContestNotFoundException e) {
            return List.of();
        }
        return problems;
    }
}
