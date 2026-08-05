package co.uceva.problem.infrastructure.web.dto;

public record CreateTestCaseRequestDTO(
                int orderIndex,
                String input,
                String output,
                String expectedOutput,
                boolean isSample) {
}