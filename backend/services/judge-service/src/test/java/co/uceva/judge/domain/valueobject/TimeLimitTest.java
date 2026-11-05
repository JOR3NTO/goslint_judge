package co.uceva.judge.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeLimitTest {

    @Test
    void shouldCreateValidTimeLimit() {
        TimeLimit timeLimit = new TimeLimit(1_000);

        assertThat(timeLimit.milliseconds()).isEqualTo(1_000);
        assertThat(timeLimit.toSeconds()).isEqualTo(1.0);
    }

    @Test
    void shouldRejectBelowMinimum() {
        assertThatThrownBy(() -> new TimeLimit(TimeLimit.MIN_MS - 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de tiempo");
    }

    @Test
    void shouldRejectAboveMaximum() {
        assertThatThrownBy(() -> new TimeLimit(TimeLimit.MAX_MS + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de tiempo");
    }

    @Test
    void shouldAcceptBoundaryValues() {
        assertThat(new TimeLimit(TimeLimit.MIN_MS).milliseconds()).isEqualTo(TimeLimit.MIN_MS);
        assertThat(new TimeLimit(TimeLimit.MAX_MS).milliseconds()).isEqualTo(TimeLimit.MAX_MS);
    }
}
