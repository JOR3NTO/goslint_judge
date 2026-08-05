package co.uceva.problem.infrastructure.web.dto;

public record UpdateTestCaseRequestDTO(
        String input,
        int orderIndex,
        String output,
        String expectedOutput,
        boolean isSample
) {}