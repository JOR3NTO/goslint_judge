package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Problem {
    private UUID id;
    private UUID createdBy;
    private String title;
    private String statement;
    private TimeLimit timeLimitMs;
    private MemoryLimit memoryLimitKb;
    private Difficulty difficult;
    private Instant createdAt;
    private String inputFormat;
    private String outputFormat;

    @Builder
    private Problem(UUID id, UUID createdBy, String title, String statement, int timeLimitMs, int memoryLimitKb,
            int difficult, Instant createdAt, String inputFormat, String outputFormat) {
        this.id = id;
        this.createdBy = createdBy;
        this.title = title;
        this.statement = statement;
        this.timeLimitMs = new TimeLimit(timeLimitMs);
        this.memoryLimitKb = new MemoryLimit(memoryLimitKb);
        this.difficult = new Difficulty(difficult);
        this.createdAt = createdAt;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    public static Problem create(UUID createdBy, String title, String statement, int timeLimitMs, int memoryLimitKb,
            int difficult, String inputFormat, String outputFormat) {
        return Problem.builder()
                .id(UUID.randomUUID())
                .createdBy(createdBy)
                .title(title)
                .statement(statement)
                .timeLimitMs(timeLimitMs)
                .memoryLimitKb(memoryLimitKb)
                .difficult(difficult)
                .createdAt(Instant.now())
                .inputFormat(inputFormat)
                .outputFormat(outputFormat)
                .build();
    }

    public void update(String title, String statement, TimeLimit timeLimitMs, MemoryLimit memoryLimitKb,
            Difficulty difficult, String inputFormat, String outputFormat) {
        this.title = title;
        this.statement = statement;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitKb = memoryLimitKb;
        this.difficult = difficult;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    public int getTimeLimitMs() {
        return timeLimitMs.milliseconds();
    }

    public void setTimeLimitMs(int timeLimitMs) {
        this.timeLimitMs = new TimeLimit(timeLimitMs);
    }

    public int getMemoryLimitKb() {
        return memoryLimitKb.kilobytes();
    }

    public void setMemoryLimitKb(int memoryLimitKb) {
        this.memoryLimitKb = new MemoryLimit(memoryLimitKb);
    }

    public int getDifficult() {
        return difficult.difficult();
    }

    public void setDifficult(int difficult) {
        this.difficult = new Difficulty(difficult);
    }
}
