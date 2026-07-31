package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Problem {
    private UUID id;
    private UUID createdBy;
    private String title;
    private String fileUrl;
    private int timeLimitMs;
    private int memoryLimitKb;
    private int difficult;
    private Instant createdAt;
    private String inputFormat;
    private String outputFormat;
    
    public Problem() {
    }

    public Problem(UUID id, UUID createdBy, String title, String fileUrl, int timeLimitMs, int memoryLimitKb,
            int difficult, Instant createdAt, String inputFormat, String outputFormat) {
        this.id = id;
        this.createdBy = createdBy;
        this.title = title;
        this.fileUrl = fileUrl;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitKb = memoryLimitKb;
        this.difficult = difficult;
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

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(int timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public int getMemoryLimitKb() {
        return memoryLimitKb;
    }

    public void setMemoryLimitKb(int memoryLimitKb) {
        this.memoryLimitKb = memoryLimitKb;
    }

    public int getDifficult() {
        return difficult;
    }

    public void setDifficult(int difficult) {
        this.difficult = difficult;
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
