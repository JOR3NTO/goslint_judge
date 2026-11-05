package co.uceva.submission.infrastructure.web.dto;

/**
 * DTO de respuesta que expone un mensaje de error al cliente.
 *
 * @param message Descripción legible del error ocurrido.
 */
public record ErrorResponseDTO(String message) {}
