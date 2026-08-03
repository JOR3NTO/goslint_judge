package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

public class TestCase {
    private UUID id;
    private UUID problemId;
    private String expectedOutput;
    private int orderIndex;
    private boolean isSample;
    private String input;
    private String output;
    private Instant createdAt;
    
    public TestCase() {
    }

    public TestCase(UUID id, UUID problemId, String expectedOutput, int orderIndex, boolean isSample, String input,
            String output, Instant createdAt) {
        this.id = id;
        this.problemId = problemId;
        this.expectedOutput = expectedOutput;
        this.orderIndex = orderIndex;
        this.isSample = isSample;
        this.input = input;
        this.output = output;
        this.createdAt = createdAt;
    }

    public static TestCase create(UUID problemId, String expectedOutput, int orderIndex, boolean isSample, String input,
            String output){
        return new TestCase(
                UUID.randomUUID(),
                problemId,
                expectedOutput,
                orderIndex,
                isSample,
                input,
                output,
                Instant.now()
        );
    }

    public void update(String expectedOutput, int orderIndex, boolean isSample, String input,
            String output){
        this.expectedOutput = expectedOutput;
        this.orderIndex = orderIndex;
        this.isSample = isSample;
        this.input = input;
        this.output = output;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProblemId() {
        return problemId;
    }

    public void setProblemId(UUID problemId) {
        this.problemId = problemId;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isSample() {
        return isSample;
    }

    public void setSample(boolean isSample) {
        this.isSample = isSample;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
