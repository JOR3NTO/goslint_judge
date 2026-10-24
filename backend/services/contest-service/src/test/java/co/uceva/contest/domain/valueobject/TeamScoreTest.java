package co.uceva.contest.domain.valueobject;

import co.uceva.contest.domain.exception.InvalidTeamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests para el Value Object TeamScore")
class TeamScoreTest {

    @Test
    @DisplayName("Debe instanciar TeamScore válido y constante ZERO")
    void shouldCreateValidTeamScore() {
        TeamScore score = TeamScore.of(5, 120L);

        assertThat(score.score()).isEqualTo(5);
        assertThat(score.penaltyTime()).isEqualTo(120L);

        assertThat(TeamScore.ZERO.score()).isZero();
        assertThat(TeamScore.ZERO.penaltyTime()).isZero();
    }

    @Test
    @DisplayName("Debe cumplir con equals, hashCode y toString")
    void shouldSupportEqualsHashCodeAndToString() {
        TeamScore s1 = TeamScore.of(5, 120L);
        TeamScore s2 = TeamScore.of(5, 120L);

        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        assertThat(s1.toString()).contains("5", "120");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el puntaje es negativo")
    void shouldThrowExceptionWhenScoreIsNegative() {
        assertThatThrownBy(() -> TeamScore.of(-1, 50L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El puntaje no puede ser negativo.");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el tiempo de penalización es negativo")
    void shouldThrowExceptionWhenPenaltyTimeIsNegative() {
        assertThatThrownBy(() -> TeamScore.of(3, -10L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El tiempo de penalización no puede ser negativo.");
    }

    @Test
    @DisplayName("Debe sumar puntaje y penalización correctamente")
    void shouldAddScoreAndPenalty() {
        TeamScore initial = TeamScore.of(2, 60L);
        TeamScore updated = initial.add(1, 45L);

        assertThat(updated.score()).isEqualTo(3);
        assertThat(updated.penaltyTime()).isEqualTo(105L);
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intentan sumar valores negativos")
    void shouldThrowExceptionWhenAddingNegativeValues() {
        TeamScore score = TeamScore.of(2, 60L);

        assertThatThrownBy(() -> score.add(-1, 10L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("deben ser no negativos");

        assertThatThrownBy(() -> score.add(1, -5L))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("deben ser no negativos");
    }
}

