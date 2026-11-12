package co.uceva.contests_platform.contest.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.uceva.contests_platform.contest.domain.valueobject.BalloonColor;
import co.uceva.contests_platform.contest.domain.valueobject.TeamScore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamStanding: puntaje y penalización ICPC desde los envíos")
class TeamStandingTest {

    private static final UUID CONTEST = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");

    private static ContestProblem problem(String alias, int points, int penalty) {
        return ContestProblem.builder().contestId(CONTEST).problemId(UUID.randomUUID()).alias(alias)
                .orderIndex(0).points(points).penalty(penalty).balloonColor(BalloonColor.of("#FF0000")).build();
    }

    private static TeamSubmission accepted(ContestProblem p, long minute) {
        return new TeamSubmission(p.getId(), true, false, START.plusSeconds(minute * 60));
    }

    private static TeamSubmission rejected(ContestProblem p, long minute, boolean penalizes) {
        return new TeamSubmission(p.getId(), false, penalizes, START.plusSeconds(minute * 60));
    }

    @Test
    @DisplayName("Cuenta el primer aceptado, sus intentos fallidos previos y los puntos del problema")
    void calculatesIcpcStanding() {
        ContestProblem a = problem("A", 1, 20);
        ContestProblem b = problem("B", 2, 10);
        ContestProblem c = problem("C", 1, 20);

        TeamStanding standing = TeamStanding.calculate(List.of(
                accepted(b, 40),               // llega antes en la lista, pero se ordena por hora
                rejected(b, 1, true),          // TLE previo: +10
                rejected(a, 5, true),          // WA previo: +20
                rejected(a, 6, false),         // error de compilación: no penaliza
                accepted(a, 10),
                accepted(a, 50),               // reenvío aceptado: no cuenta
                rejected(a, 55, true),         // posterior al aceptado: no cuenta
                rejected(c, 30, true)),        // problema sin resolver: no cuenta
                List.of(a, b, c), START);

        // A: 10 + 20; B: 40 + 10.
        assertThat(standing.teamScore()).isEqualTo(TeamScore.of(3, 80));
        assertThat(standing.lastSolvedAt()).isEqualTo(START.plusSeconds(40 * 60));
    }

    @Test
    @DisplayName("Sin aceptados no hay puntaje ni hora de último resuelto")
    void noAcceptedSubmissions() {
        ContestProblem a = problem("A", 1, 20);

        TeamStanding standing = TeamStanding.calculate(List.of(rejected(a, 5, true)), List.of(a), START);

        assertThat(standing.teamScore()).isEqualTo(TeamScore.ZERO);
        assertThat(standing.lastSolvedAt()).isNull();
    }
}
