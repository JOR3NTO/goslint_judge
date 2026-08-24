package co.uceva.problem.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifficultyTest {

    @ParameterizedTest
    @ValueSource(ints = {800, 1000, 1500, 2000, 3500})
    void shouldCreateValidDifficulty(int value) {
        Difficulty difficulty = new Difficulty(value);

        assertThat(difficulty.difficult()).isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({
            "700, estar entre",
            "3600, estar entre",
            "850, múltiplo de 100",
            "1234, múltiplo de 100"
    })
    void shouldRejectInvalidDifficulty(int value, String expectedMessage) {
        assertThatThrownBy(() -> new Difficulty(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }
}
