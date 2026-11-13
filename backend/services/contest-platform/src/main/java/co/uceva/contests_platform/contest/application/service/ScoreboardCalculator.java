package co.uceva.contests_platform.contest.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import co.uceva.contests_platform.contest.domain.model.Contest;
import co.uceva.contests_platform.contest.domain.model.ContestProblem;
import co.uceva.contests_platform.contest.domain.model.ScoreboardRow;
import co.uceva.contests_platform.contest.domain.model.TeamStanding;
import co.uceva.contests_platform.contest.domain.model.TeamSubmission;
import co.uceva.contests_platform.contest.domain.repository.ContestProblemRepository;
import co.uceva.contests_platform.contest.domain.repository.TeamRepository;
import co.uceva.contests_platform.contest.infrastructure.client.SubmissionAdapter;

/**
 * Cálculo completo de la tabla de posiciones de un concurso desde la base de datos.
 */
@Component
public class ScoreboardCalculator {

    private final TeamRepository teamRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final SubmissionAdapter submissionAdapter;

    public ScoreboardCalculator(TeamRepository teamRepository, ContestProblemRepository contestProblemRepository,
            SubmissionAdapter submissionAdapter) {
        this.teamRepository = teamRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.submissionAdapter = submissionAdapter;
    }

    /**
     * Tabla ordenada del concurso.
     *
     * @param contest Concurso.
     * @param cutoff  Sin corte (nulo) usa el puntaje guardado de cada equipo. Con corte recalcula el puntaje
     *                solo con los envíos anteriores a ese momento: es la tabla congelada, y así sale igual
     *                aunque la aplicación se reinicie durante el congelamiento.
     * @return Filas ordenadas.
     */
    public List<ScoreboardRow> build(Contest contest, Instant cutoff) {
        List<ContestProblem> problems = contestProblemRepository.findAllByContestIdOrderByOrderIndexAsc(contest.getId());
        return teamRepository.findAllByContestId(contest.getId()).stream()
                .map(team -> {
                    List<TeamSubmission> submissions = submissionAdapter.findByTeamId(team.getId());
                    if (cutoff != null) {
                        submissions = submissions.stream().filter(s -> s.submittedAt().isBefore(cutoff)).toList();
                    }
                    TeamStanding standing = TeamStanding.calculate(submissions, problems, contest.getStartTime());
                    if (cutoff != null) {
                        team.updateScore(standing.teamScore());
                    }
                    return new ScoreboardRow(team, standing.lastSolvedAt());
                })
                .sorted(ScoreboardRow.ORDER)
                .toList();
    }
}
