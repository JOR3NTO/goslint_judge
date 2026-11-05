package co.uceva.judge.infrastructure.client.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Problema tal como lo expone {@code problem-service}; solo los campos que el
 * juez necesita.
 *
 * @param id            Identificador del problema.
 * @param timeLimitMs   Límite de tiempo en milisegundos.
 * @param memoryLimitKb Límite de memoria en kilobytes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemResponse(UUID id, int timeLimitMs, int memoryLimitKb) {}
