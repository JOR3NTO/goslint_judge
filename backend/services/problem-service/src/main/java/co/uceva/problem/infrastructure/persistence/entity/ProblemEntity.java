package co.uceva.problem.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "problems")
public class ProblemEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "title", nullable = false, updatable = true)
    private String title;

    @Column(name = "statement", nullable = false, updatable = true)
    private String statement;

    @Column(name = "time_limit_ms", nullable = false, updatable = true)
    private int timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false, updatable = true)
    private int memoryLimitKb;

    @Column(name = "difficulty", nullable = false, updatable = true)
    private int difficult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "input_format", nullable = false, updatable = true)
    private String inputFormat;

    @Column(name = "output_format", nullable = false, updatable = true)
    private String outputFormat;
}
