package co.uceva.contest.domain.valueobject;

import co.uceva.contest.domain.exception.InvalidContestProblemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests para el Value Object BalloonColor")
class BalloonColorTest {

    @Test
    @DisplayName("Debe normalizar el color a mayúsculas y sin espacios")
    void shouldNormalizeHex() {
        BalloonColor color = BalloonColor.of("  #ff00aa ");

        assertThat(color.value()).isEqualTo("#FF00AA");
        assertThat(color.getValue()).isEqualTo("#FF00AA");
        assertThat(color.toString()).isEqualTo("#FF00AA");
        assertThat(color).isEqualTo(BalloonColor.of("#FF00AA"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el color es nulo")
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> BalloonColor.of(null))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "red", "FF0000", "#FFF", "#FF00AG", "#FF00AA00"})
    @DisplayName("Debe lanzar excepción si el color no es #RRGGBB")
    void shouldThrowWhenInvalidFormat(String hex) {
        assertThatThrownBy(() -> BalloonColor.of(hex))
                .isInstanceOf(InvalidContestProblemException.class)
                .hasMessageContaining("#RRGGBB");
    }
}
