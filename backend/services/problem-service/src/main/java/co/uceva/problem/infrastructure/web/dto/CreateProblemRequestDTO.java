package co.uceva.problem.infrastructure.web.dto;

import java.util.UUID;

public record CreateProblemRequestDTO(
    UUID createdBy,
    String title,
    String statement,
    int timeLimitMs,
    int memoryLimitKb,
    int difficult,
    String inputFormat,
    String outputFormat
) {}
