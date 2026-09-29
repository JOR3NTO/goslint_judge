package co.uceva.judge.domain.model;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.uceva.shared.domain.ProgrammingLanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JudgeTaskTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final List<TestCase> TEST_CASES = List.of(
            new TestCase(UUID.randomUUID(), "1 2", "3", 0)
    );

    @Test
    void shouldCreateValidJudgeTask() {
        JudgeTask task = JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "print(1)", TEST_CASES, 1_000, 8_192
        );

        assertThat(task.getSubmissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(task.getLanguage()).isEqualTo(ProgrammingLanguage.PYTHON);
        assertThat(task.getSourceCode().content()).isEqualTo("print(1)");
        assertThat(task.getTestCases()).isEqualTo(TEST_CASES);
        assertThat(task.getTimeLimit().milliseconds()).isEqualTo(1_000);
        assertThat(task.getMemoryLimit().kilobytes()).isEqualTo(8_192);
    }

    @Test
    void shouldRejectNullSubmissionId() {
        assertThatThrownBy(() -> JudgeTask.create(
                null, ProgrammingLanguage.PYTHON, "print(1)", TEST_CASES, 1_000, 8_192
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador del envío");
    }

    @Test
    void shouldRejectEmptyTestCases() {
        assertThatThrownBy(() -> JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "print(1)", List.of(), 1_000, 8_192
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un caso de prueba");
    }

    @Test
    void shouldRejectNullTestCases() {
        assertThatThrownBy(() -> JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "print(1)", null, 1_000, 8_192
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un caso de prueba");
    }

    @Test
    void shouldPropagateInvalidSourceCode() {
        assertThatThrownBy(() -> JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "   ", TEST_CASES, 1_000, 8_192
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código fuente no puede estar vacío");
    }

    @Test
    void shouldPropagateInvalidTimeLimit() {
        assertThatThrownBy(() -> JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "print(1)", TEST_CASES, 0, 8_192
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de tiempo");
    }

    @Test
    void shouldPropagateInvalidMemoryLimit() {
        assertThatThrownBy(() -> JudgeTask.create(
                SUBMISSION_ID, ProgrammingLanguage.PYTHON, "print(1)", TEST_CASES, 1_000, 0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("límite de memoria");
    }
}
