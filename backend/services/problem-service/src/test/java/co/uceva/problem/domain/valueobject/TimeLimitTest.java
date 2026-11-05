package co.uceva.problem.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeLimitTest {

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000, 5000})
    void shouldCreateValidTimeLimit(int value) {
        TimeLimit timeLimit = new TimeLimit(value);

        assertThat(timeLimit.milliseconds()).isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({
            "99, 100 ms",
            "5001, 5000 ms"
    })
    void shouldRejectInvalidTimeLimit(int value, String expectedMessage) {
        assertThatThrownBy(() -> new TimeLimit(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void shouldConvertToSeconds() {
        assertThat(new TimeLimit(1500).toSeconds()).isEqualTo(1.5);
    }
}
