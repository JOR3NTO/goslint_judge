package co.uceva.contests_platform.contest.application.usecase.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.GetScoreboardUseCase;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar la tabla de posiciones de un concurso (mayor puntaje, menor penalización y, como
 * segundo desempate, quien resolvió antes su último problema).
 */
@Service
public class GetScoreboardUseCaseImpl implements GetScoreboardUseCase {

    private final TeamRepository teamRepository;
    private final SubmissionAdapter submissionAdapter;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository    Puerto de salida para consultar equipos.
     * @param submissionAdapter Adaptador para consultar los envíos de cada equipo.
     */
    public GetScoreboardUseCaseImpl(TeamRepository teamRepository, SubmissionAdapter submissionAdapter) {
        this.teamRepository = teamRepository;
        this.submissionAdapter = submissionAdapter;
    }

    /**
     * Consultar la tabla de posiciones de un concurso (mayor puntaje y menor penalización primero).
     *
     * @param contestId Identificador del concurso.
     * @return Lista de resultados.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Team> execute(UUID contestId) {
        List<Team> teams = teamRepository.findAllByContestId(contestId);
        // Equipos sin problemas resueltos no tienen hora: quedan al final de su empate.
        Map<UUID, Instant> lastSolvedAt = teams.stream().collect(Collectors.toMap(Team::getId,
                t -> submissionAdapter.findLastSolvedAt(t.getId()).orElse(Instant.MAX)));
        return teams.stream()
                .sorted(Comparator.comparing((Team t) -> t.getTeamScore().score()).reversed()
                        .thenComparing(t -> t.getTeamScore().penaltyTime())
                        .thenComparing(t -> lastSolvedAt.get(t.getId())))
                .toList();
    }
}
