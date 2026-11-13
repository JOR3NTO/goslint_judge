package co.uceva.contests_platform.contest.application.usecase.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.uceva.contests_platform.contest.application.service.ScoreboardCalculator;
import co.uceva.contests_platform.contest.application.usecase.GetScoreboardUseCase;
import co.uceva.contests_platform.contest.domain.exception.ContestNotFoundException;
import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.Team;
import co.uceva.contests_platform.contest.domain.repository.ContestRepository;
import co.uceva.contests_platform.contest.infrastructure.scoreboard.ScoreboardCache;

/**
 * Servicio de Aplicación que implementa el caso de uso: consultar la tabla de posiciones de un concurso (mayor puntaje, menor penalización y, como
 * segundo desempate, quien resolvió antes su último problema).
 * <p>
 * Se sirve desde la caché; solo si el concurso no está en ella se calcula completo desde la base de datos.
 * Con la tabla congelada se calcula hasta la hora de congelamiento, así sale igual tras un reinicio.
 * </p>
 */
@Service
public class GetScoreboardUseCaseImpl implements GetScoreboardUseCase {

    private final ContestRepository contestRepository;
    private final ScoreboardCalculator scoreboardCalculator;
    private final ScoreboardCache scoreboardCache;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param contestRepository    Puerto de salida para consultar el concurso.
     * @param scoreboardCalculator Cálculo completo de la tabla desde la base de datos.
     * @param scoreboardCache      Tabla de posiciones en memoria.
     */
    public GetScoreboardUseCaseImpl(ContestRepository contestRepository, ScoreboardCalculator scoreboardCalculator,
            ScoreboardCache scoreboardCache) {
        this.contestRepository = contestRepository;
        this.scoreboardCalculator = scoreboardCalculator;
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

    private ScoreboardCache.Board build(UUID contestId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId));
        return contest.isScoreboardFrozen()
                ? new ScoreboardCache.Board(scoreboardCalculator.build(contest, contest.getFreezeTime()), true)
                : new ScoreboardCache.Board(scoreboardCalculator.build(contest, null), false);
    }
}
