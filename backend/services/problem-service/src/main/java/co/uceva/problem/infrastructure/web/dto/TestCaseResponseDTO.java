package co.uceva.problem.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record TestCaseResponseDTO(
        UUID id,
        UUID problemId,
        String input,
        String output,
        String expectedOutput,
        int orderIndex,
        boolean isSample,
        Instant createdAt
) {}