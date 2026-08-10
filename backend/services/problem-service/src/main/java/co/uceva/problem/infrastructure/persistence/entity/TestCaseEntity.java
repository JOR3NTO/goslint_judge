package co.uceva.problem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "test_cases")
public class TestCaseEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "problem_id", nullable = false, updatable = false)
    private UUID problemId;

    @Column(name = "expected_output", nullable = false, updatable = true)
    private String expectedOutput;

    @Column(name = "order_index", nullable = false, updatable = true)
    private int orderIndex;

    @Column(name = "is_sample", nullable = false, updatable = true)
    private boolean isSample;

    @Column(name = "input", nullable = false, updatable = true)
    private String input;

    @Column(name = "output", nullable = false, updatable = true)
    private String output;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
