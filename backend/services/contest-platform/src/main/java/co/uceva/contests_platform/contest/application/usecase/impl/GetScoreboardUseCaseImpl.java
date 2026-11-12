package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.usecase.GetScoreboardUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.model.TeamStanding;
import co.uceva.contests_platform.contest.domain.repository.ContestProblemRepository;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;
import co.uceva.contests_platform.contest.infrastructure.scoreboard.ScoreboardCache;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar la tabla de posiciones de un concurso (mayor puntaje, menor penalización y, como
 * segundo desempate, quien resolvió antes su último problema).
 * <p>
 * Se sirve desde la caché; solo si el concurso no está en ella se calcula completo desde la base de datos.
 * </p>
 */
@Service
public class GetScoreboardUseCaseImpl implements GetScoreboardUseCase {

    private final TeamRepository teamRepository;
    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final SubmissionAdapter submissionAdapter;
    private final ScoreboardCache scoreboardCache;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param teamRepository           Puerto de salida para consultar equipos.
     * @param contestRepository        Puerto de salida para consultar el inicio del concurso.
     * @param contestProblemRepository Puerto de salida para consultar los problemas del concurso.
     * @param submissionAdapter        Adaptador para consultar los envíos de cada equipo.
     * @param scoreboardCache          Tabla de posiciones en memoria.
     */
    public GetScoreboardUseCaseImpl(TeamRepository teamRepository, ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository, SubmissionAdapter submissionAdapter,
            ScoreboardCache scoreboardCache) {
        this.teamRepository = teamRepository;
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.submissionAdapter = submissionAdapter;
        this.scoreboardCache = scoreboardCache;
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
        return scoreboardCache.getOrBuild(contestId, this::build).stream().map(ScoreboardRow::team).toList();
    }

    /** Cálculo completo desde la base de datos: puntaje guardado y hora del último problema resuelto. */
    private List<ScoreboardRow> build(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId));
        List<ContestProblem> problems = contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contestId);
        return teamRepository.findAllByContestId(contestId).stream()
                .map(t -> new ScoreboardRow(t, TeamStanding.calculate(
                        submissionAdapter.findByTeamId(t.getId()), problems, contest.getStartTime()).lastSolvedAt()))
                .sorted(ScoreboardRow.ORDER)
                .toList();
    }
}
