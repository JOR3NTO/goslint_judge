package co.uceva.problem.domain.model;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemTest {

    @Test
    void shouldCreateProblem() {
        Problem problem = Problem.create(
                UUID.randomUUID(),
                "Suma",
                "Statement",
                1000,
                65536,
                1200,
                "input",
                "output"
        );

        Instant now = Instant.now();
        assertThat(problem.getId()).isNotNull();
        assertThat(problem.getCreatedAt()).isBetween(now.minusSeconds(1), now.plusSeconds(1));
        assertThat(problem.getTitle()).isEqualTo("Suma");
        assertThat(problem.getTimeLimitMs()).isEqualTo(1000);
        assertThat(problem.getMemoryLimitKb()).isEqualTo(65536);
        assertThat(problem.getDifficult()).isEqualTo(1200);
    }

    @Test
    void shouldUpdateProblemFields() {
        Problem problem = Problem.create(
                UUID.randomUUID(),
                "Original",
                "Original statement",
                1000,
                65536,
                800,
                "original input",
                "original output"
        );

        problem.update(
                "Updated",
                "Updated statement",
                new TimeLimit(2000),
                new MemoryLimit(131072),
                new Difficulty(1500),
                "updated input",
                "updated output"
        );

        assertThat(problem.getTitle()).isEqualTo("Updated");
        assertThat(problem.getStatement()).isEqualTo("Updated statement");
        assertThat(problem.getTimeLimitMs()).isEqualTo(2000);
        assertThat(problem.getMemoryLimitKb()).isEqualTo(131072);
        assertThat(problem.getDifficult()).isEqualTo(1500);
        assertThat(problem.getInputFormat()).isEqualTo("updated input");
        assertThat(problem.getOutputFormat()).isEqualTo("updated output");
    }

    @Test
    void shouldWrapValueObjectsOnSetters() {
        Problem problem = new Problem();

        problem.setTimeLimitMs(2500);
        problem.setMemoryLimitKb(8192);
        problem.setDifficult(900);

        assertThat(problem.getTimeLimitMs()).isEqualTo(2500);
        assertThat(problem.getMemoryLimitKb()).isEqualTo(8192);
        assertThat(problem.getDifficult()).isEqualTo(900);
    }
}
