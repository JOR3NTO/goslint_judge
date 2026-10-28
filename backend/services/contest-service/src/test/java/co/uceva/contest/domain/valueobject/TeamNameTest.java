package co.uceva.contest.domain.valueobject;

import co.uceva.contest.domain.exception.InvalidTeamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests para el Value Object TeamName")
class TeamNameTest {

    @Test
    @DisplayName("Debe crear un TeamName válido y limpiar espacios en blanco")
    void shouldCreateValidTeamName() {
        TeamName teamName = TeamName.of("  Los Programadores  ");

        assertThat(teamName.value()).isEqualTo("Los Programadores");
        assertThat(teamName.getValue()).isEqualTo("Los Programadores");
        assertThat(teamName.toString()).isEqualTo("Los Programadores");
    }

    @Test
    @DisplayName("Debe cumplir con equals y hashCode")
    void shouldSupportEqualsAndHashCode() {
        TeamName t1 = TeamName.of("Los Programadores");
        TeamName t2 = TeamName.of("Los Programadores");

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    @DisplayName("Debe aceptar nombres en los límites de longitud (3 y 50 caracteres)")
    void shouldAcceptBoundaryLengths() {
        TeamName minLength = TeamName.of("ABC");
        String maxChars = "A".repeat(50);
        TeamName maxLength = TeamName.of(maxChars);

        assertThat(minLength.value()).hasSize(3);
        assertThat(maxLength.value()).hasSize(50);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre es nulo")
    void shouldThrowExceptionWhenNull() {
        assertThatThrownBy(() -> new TeamName(null))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El nombre del equipo no puede estar vacío.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("Debe lanzar excepción si el nombre está en blanco")
    void shouldThrowExceptionWhenBlank(String blankName) {
        assertThatThrownBy(() -> TeamName.of(blankName))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("El nombre del equipo no puede estar vacío.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "AB", "  xy  "})
    @DisplayName("Debe lanzar excepción si el nombre tiene menos de 3 caracteres")
    void shouldThrowExceptionWhenTooShort(String shortName) {
        assertThatThrownBy(() -> TeamName.of(shortName))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("debe tener entre 3 y 50 caracteres.");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre supera los 50 caracteres")
    void shouldThrowExceptionWhenTooLong() {
        String longName = "A".repeat(51);

        assertThatThrownBy(() -> TeamName.of(longName))
                .isInstanceOf(InvalidTeamException.class)
                .hasMessageContaining("debe tener entre 3 y 50 caracteres.");
    }
}

