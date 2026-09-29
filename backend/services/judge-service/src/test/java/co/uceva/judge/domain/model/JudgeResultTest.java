package co.uceva.judge.domain.model;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.uceva.shared.domain.VerdictStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JudgeResultTest {

    private static final UUID SUBMISSION_ID = UUID.randomUUID();

    @Test
    void shouldCreateAcceptedResult() {
        JudgeResult result = JudgeResult.create(SUBMISSION_ID, VerdictStatus.ACCEPTED, 120, 2_048, null);

        assertThat(result.getSubmissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(result.getExecutionTimeMs()).isEqualTo(120);
        assertThat(result.getMemoryUsedKb()).isEqualTo(2_048);
        assertThat(result.getFailedTestCase()).isNull();
    }

    @Test
    void shouldCreateRejectedResultWithFailedTestCase() {
        UUID failedTestCase = UUID.randomUUID();

        JudgeResult result = JudgeResult.create(
                SUBMISSION_ID, VerdictStatus.WRONG_ANSWER, 80, 1_024, failedTestCase
        );

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.WRONG_ANSWER);
        assertThat(result.getFailedTestCase()).isEqualTo(failedTestCase);
    }

    @Test
    void shouldCreateRejectedResultWithoutFailedTestCase() {
        JudgeResult result = JudgeResult.create(
                SUBMISSION_ID, VerdictStatus.COMPILATION_ERROR, 0, 0, null
        );

        assertThat(result.getVerdict()).isEqualTo(VerdictStatus.COMPILATION_ERROR);
        assertThat(result.getFailedTestCase()).isNull();
    }

    @Test
    void shouldRejectAcceptedVerdictWithFailedTestCase() {
        assertThatThrownBy(() -> JudgeResult.create(
                SUBMISSION_ID, VerdictStatus.ACCEPTED, 0, 0, UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede tener un caso de prueba fallido");
    }

    @Test
    void shouldRejectNullSubmissionId() {
        assertThatThrownBy(() -> JudgeResult.create(null, VerdictStatus.ACCEPTED, 0, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador del envío");
    }

    @Test
    void shouldRejectNullVerdict() {
        assertThatThrownBy(() -> JudgeResult.create(SUBMISSION_ID, null, 0, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("veredicto de la evaluación");
    }

    @Test
    void shouldRejectNegativeExecutionTime() {
        assertThatThrownBy(() -> JudgeResult.create(SUBMISSION_ID, VerdictStatus.ACCEPTED, -1, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tiempo de ejecución");
    }

    @Test
    void shouldRejectNegativeMemoryUsed() {
        assertThatThrownBy(() -> JudgeResult.create(SUBMISSION_ID, VerdictStatus.ACCEPTED, 0, -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memoria utilizada");
    }
}
