package co.uceva.contests_platform.contest.infrastructure.web.dto;

/**
 * DTO de respuesta para errores de la API.
 *
 * @param message Descripción del error.
 */
public record ErrorResponseDTO(String message) {}
