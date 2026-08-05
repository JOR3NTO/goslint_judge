package co.uceva.problem.infrastructure.web.dto;

public record UpdateProblemRequestDTO(
    String title,
    String statement,
    int timeLimitMs,
    int memoryLimitKb,
    int difficult,
    String inputFormat,
    String outputFormat
) {}