package co.uceva.contest.domain.model;

import co.uceva.contest.domain.exception.InvalidContestProblemException;
import co.uceva.contest.domain.valueobject.BalloonColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitarios para la entidad ContestProblem")
class ContestProblemTest {

    private static final BalloonColor RED = BalloonColor.of("#FF0000");

    private static ContestProblem newProblem() {
        return ContestProblem.create(UUID.randomUUID(), UUID.randomUUID(), "A", 0, RED);
    }

    @Test
    @DisplayName("Debe crear ContestProblem con puntos por defecto y alias en mayúsculas")
    void shouldCreateWithFactoryMethod() {
        UUID contestId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        ContestProblem problem = ContestProblem.create(contestId, problemId, " b ", 1, RED);

        assertThat(problem.getId()).isNotNull();
        assertThat(problem.getContestId()).isEqualTo(contestId);
        assertThat(problem.getProblemId()).isEqualTo(problemId);
        assertThat(problem.getAlias()).isEqualTo("B");
        assertThat(problem.getOrderIndex()).isEqualTo(1);
        assertThat(problem.getPoints()).isEqualTo(ContestProblem.DEFAULT_POINTS);
        assertThat(problem.getPenalty()).isEqualTo(ContestProblem.DEFAULT_PENALTY);
        assertThat(problem.getBalloonColor()).isEqualTo(RED);
    }

    @Test
    @DisplayName("Debe actualizar alias, posición, puntos y color del globo")
    void shouldUpdate() {
        ContestProblem problem = newProblem();
        BalloonColor blue = BalloonColor.of("#0000FF");

        problem.update("c", 2, 5, 10, blue);

        assertThat(problem.getAlias()).isEqualTo("C");
        assertThat(problem.getOrderIndex()).isEqualTo(2);
        assertThat(problem.getPoints()).isEqualTo(5);
        assertThat(problem.getPenalty()).isEqualTo(10);
        assertThat(problem.getBalloonColor()).isEqualTo(blue);
    }

    @Test
    @DisplayName("Debe exigir concurso y problema")
    void shouldRequireIds() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> ContestProblem.create(null, id, "A", 0, RED))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("concurso");
        assertThatThrownBy(() -> ContestProblem.create(id, null, "A", 0, RED))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("problema");
    }

    @Test
    @DisplayName("Debe rechazar atributos inválidos sin modificar el estado")
    void shouldRejectInvalidUpdate() {
        ContestProblem problem = newProblem();

        assertThatThrownBy(() -> problem.update("  ", 0, 1, 20, RED))
                .isInstanceOf(InvalidContestProblemException.class);
        assertThatThrownBy(() -> problem.update("A".repeat(11), 0, 1, 20, RED))
                .isInstanceOf(InvalidContestProblemException.class);
        assertThatThrownBy(() -> problem.update("A", -1, 1, 20, RED))
                .isInstanceOf(InvalidContestProblemException.class);
        assertThatThrownBy(() -> problem.update("A", 0, 0, 20, RED))
                .isInstanceOf(InvalidContestProblemException.class);
        assertThatThrownBy(() -> problem.update("A", 0, 1, -1, RED))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("penalización");
        assertThatThrownBy(() -> problem.update("Z", 9, 9, 20, null))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("globo");

        assertThat(problem.getAlias()).isEqualTo("A");
        assertThat(problem.getOrderIndex()).isZero();
        assertThat(problem.getPoints()).isEqualTo(1);
        assertThat(problem.getPenalty()).isEqualTo(20);
    }

    @Test
    @DisplayName("Debe aceptar penalización cero (sin penalización)")
    void shouldAcceptZeroPenalty() {
        ContestProblem problem = newProblem();

        problem.update("A", 0, 1, 0, RED);

        assertThat(problem.getPenalty()).isZero();
    }
}
