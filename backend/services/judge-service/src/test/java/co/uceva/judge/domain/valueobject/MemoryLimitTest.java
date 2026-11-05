package co.uceva.judge.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryLimitTest {

    @Test
    void shouldCreateValidMemoryLimit() {
        MemoryLimit memoryLimit = new MemoryLimit(8_192);

        assertThat(memoryLimit.kilobytes()).isEqualTo(8_192);
        assertThat(memoryLimit.toMegabytes()).isEqualTo(8.0);
    }

    @Test
    void shouldRejectBelowMinimum() {
        assertThatThrownBy(() -> new MemoryLimit(MemoryLimit.MIN_KB - 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de memoria");
    }

    @Test
    void shouldRejectAboveMaximum() {
        assertThatThrownBy(() -> new MemoryLimit(MemoryLimit.MAX_KB + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de memoria");
    }

    @Test
    void shouldAcceptBoundaryValues() {
        assertThat(new MemoryLimit(MemoryLimit.MIN_KB).kilobytes()).isEqualTo(MemoryLimit.MIN_KB);
        assertThat(new MemoryLimit(MemoryLimit.MAX_KB).kilobytes()).isEqualTo(MemoryLimit.MAX_KB);
    }
}
