package co.uceva.contests_platform.contest.domain.model;

import java.time.Instant;
import java.util.Comparator;

/**
 * Fila de la tabla de posiciones: el equipo y la hora de su último problema resuelto.
 *
 * @param team         Equipo con su puntaje y penalización.
 * @param lastSolvedAt Primer aceptado del último problema resuelto; nulo si no ha resuelto ninguno.
 */
public record ScoreboardRow(Team team, Instant lastSolvedAt) {

    /** Mayor puntaje, menor penalización y, como desempate, quien resolvió antes su último problema. */
    public static final Comparator<ScoreboardRow> ORDER =
            Comparator.comparing((ScoreboardRow r) -> r.team().getTeamScore().score()).reversed()
                    .thenComparing(r -> r.team().getTeamScore().penaltyTime())
                    .thenComparing(ScoreboardRow::lastSolvedAt, Comparator.nullsLast(Comparator.naturalOrder()));
}
