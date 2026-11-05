package co.uceva.problem.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseTest {

    @Test
    void shouldCreateTestCase() {
        UUID problemId = UUID.randomUUID();

        TestCase testCase = TestCase.create(
                problemId,
                "3",
                1,
                true,
                "1 2",
                "3"
        );

        Instant now = Instant.now();
        assertThat(testCase.getId()).isNotNull();
        assertThat(testCase.getProblemId()).isEqualTo(problemId);
        assertThat(testCase.getCreatedAt()).isBetween(now.minusSeconds(1), now.plusSeconds(1));
        assertThat(testCase.getOrderIndex()).isEqualTo(1);
        assertThat(testCase.isSample()).isTrue();
    }

    @Test
    void shouldUpdateTestCaseFields() {
        TestCase testCase = TestCase.create(
                UUID.randomUUID(),
                "3",
                1,
                true,
                "1 2",
                "3"
        );

        testCase.update("5", 2, false, "2 3", "5");

        assertThat(testCase.getExpectedOutput()).isEqualTo("5");
        assertThat(testCase.getOrderIndex()).isEqualTo(2);
        assertThat(testCase.isSample()).isFalse();
        assertThat(testCase.getInput()).isEqualTo("2 3");
        assertThat(testCase.getOutput()).isEqualTo("5");
    }
}
