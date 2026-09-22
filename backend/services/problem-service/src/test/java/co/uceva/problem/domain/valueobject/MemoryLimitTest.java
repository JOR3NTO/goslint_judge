package co.uceva.problem.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryLimitTest {

    @ParameterizedTest
    @ValueSource(ints = {4096, 65536, 262144})
    void shouldCreateValidMemoryLimit(int value) {
        MemoryLimit memoryLimit = new MemoryLimit(value);

        assertThat(memoryLimit.kilobytes()).isEqualTo(value);
    }

    @ParameterizedTest
    @CsvSource({
            "4095, 4 MB",
            "262145, 256 MB"
    })
    void shouldRejectInvalidMemoryLimit(int value, String expectedMessage) {
        assertThatThrownBy(() -> new MemoryLimit(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void shouldConvertToMegabytes() {
        assertThat(new MemoryLimit(8192).toMegabytes()).isEqualTo(8.0);
    }
}
