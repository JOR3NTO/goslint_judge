package co.uceva.judge.domain.model;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestCaseTest {

    @Test
    void shouldCreateValidTestCase() {
        UUID id = UUID.randomUUID();

        TestCase testCase = new TestCase(id, "1 2", "3", 0);

        assertThat(testCase.id()).isEqualTo(id);
        assertThat(testCase.input()).isEqualTo("1 2");
        assertThat(testCase.expectedOutput()).isEqualTo("3");
        assertThat(testCase.orderIndex()).isEqualTo(0);
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> new TestCase(null, "1 2", "3", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador del caso de prueba");
    }

    @Test
    void shouldRejectNullExpectedOutput() {
        assertThatThrownBy(() -> new TestCase(UUID.randomUUID(), "1 2", null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salida esperada");
    }

    @Test
    void shouldRejectNegativeOrderIndex() {
        assertThatThrownBy(() -> new TestCase(UUID.randomUUID(), "1 2", "3", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orden de ejecución");
    }
}
