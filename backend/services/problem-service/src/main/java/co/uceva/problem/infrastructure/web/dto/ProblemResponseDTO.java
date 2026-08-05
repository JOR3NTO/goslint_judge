package co.uceva.problem.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ProblemResponseDTO(
        UUID id,
        UUID createdBy,
        String title,
        String statement,
        int timeLimitMs,
        int memoryLimitKb,
        int difficult,
        Instant createdAt,
        String inputFormat,
        String outputFormat
) {}