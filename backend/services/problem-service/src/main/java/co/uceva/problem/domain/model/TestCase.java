package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TestCase {
    private UUID id;
    private UUID problemId;
    private String expectedOutput;
    private int orderIndex;
    private boolean isSample;
    private String input;
    private String output;
    private Instant createdAt;

    @Builder
    private TestCase(UUID id, UUID problemId, String expectedOutput, int orderIndex, boolean isSample, String input,
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
        return TestCase.builder()
                .id(UUID.randomUUID())
                .problemId(problemId)
                .expectedOutput(expectedOutput)
                .orderIndex(orderIndex)
                .isSample(isSample)
                .input(input)
                .output(output)
                .createdAt(Instant.now())
                .build();
    }

    public void update(String expectedOutput, int orderIndex, boolean isSample, String input,
            String output){
        this.expectedOutput = expectedOutput;
        this.orderIndex = orderIndex;
        this.isSample = isSample;
        this.input = input;
        this.output = output;
    }
}
