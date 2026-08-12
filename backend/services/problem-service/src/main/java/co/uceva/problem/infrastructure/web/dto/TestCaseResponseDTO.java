package co.uceva.problem.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta que expone los datos de un caso de prueba al cliente.
 *
 * @param id             Identificador del caso de prueba.
 * @param problemId      Identificador del problema asociado.
 * @param input          Entrada del caso.
 * @param output         Salida esperada del caso.
 * @param expectedOutput Salida esperada para la entrada.
 * @param orderIndex     Orden de ejecución.
 * @param isSample       Indica si es un caso de ejemplo público.
 * @param createdAt      Fecha de creación.
 */
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