package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.valueobject.Difficulty;
import co.uceva.problem.domain.valueobject.MemoryLimit;
import co.uceva.problem.domain.valueobject.TimeLimit;

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
    
    public Problem() {
    }

    public Problem(UUID id, UUID createdBy, String title, String statement, int timeLimitMs, int memoryLimitKb,
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}
