package co.uceva.contests_platform.contest.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import co.uceva.contests_platform.contest.domain.valueobject.TeamScore;

/**
 * Resultado de un equipo calculado desde sus envíos con la regla ICPC.
 *
 * @param teamScore    Puntaje y penalización.
 * @param lastSolvedAt Primer aceptado del último problema resuelto; nulo si no ha resuelto ninguno.
 */
public record TeamStanding(TeamScore teamScore, Instant lastSolvedAt) {

    /**
     * Por cada problema del concurso solo cuenta el primer aceptado: suma sus puntos y, como
     * penalización, los minutos desde el inicio más la penalización del problema por cada
     * intento fallido anterior. Los intentos fallidos de problemas sin resolver no cuentan.
     *
     * @param submissions  Envíos del equipo, en cualquier orden.
     * @param problems     Problemas del concurso.
     * @param contestStart Inicio del concurso.
     * @return El resultado del equipo.
     */
    public static TeamStanding calculate(List<TeamSubmission> submissions, List<ContestProblem> problems,
                                         Instant contestStart) {
        Map<UUID, ContestProblem> problemsById = new HashMap<>();
        problems.forEach(p -> problemsById.put(p.getId(), p));

        Map<UUID, Integer> failedAttempts = new HashMap<>();
        Set<UUID> solved = new HashSet<>();
        int score = 0;
        long penalty = 0;
        Instant lastSolvedAt = null;

        List<TeamSubmission> ordered = submissions.stream()
                .sorted(Comparator.comparing(TeamSubmission::submittedAt)).toList();
        for (TeamSubmission s : ordered) {
            ContestProblem problem = problemsById.get(s.problemId());
            if (problem == null || solved.contains(s.problemId())) {
                continue;
            }
            if (s.accepted()) {
                solved.add(s.problemId());
                score += problem.getPoints();
                penalty += Duration.between(contestStart, s.submittedAt()).toMinutes()
                        + (long) problem.getPenalty() * failedAttempts.getOrDefault(s.problemId(), 0);
                lastSolvedAt = s.submittedAt();
            } else if (s.penalizes()) {
                failedAttempts.merge(s.problemId(), 1, Integer::sum);
            }
        }
        return new TeamStanding(TeamScore.of(score, penalty), lastSolvedAt);
    }
}
