package co.uceva.judge.infrastructure.client.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Caso de prueba tal como lo expone {@code problem-service}; solo los campos
 * que el juez necesita.
 *
 * @param id             Identificador del caso de prueba.
 * @param input          Entrada del caso.
 * @param expectedOutput Salida esperada.
 * @param orderIndex     Orden de ejecución.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TestCaseResponse(UUID id, String input, String expectedOutput, int orderIndex) {}
