package co.uceva.problem.infrastructure.web.dto;

/**
 * DTO de solicitud para crear un caso de prueba.
 *
 * @param orderIndex     Orden de ejecución del caso.
 * @param input          Entrada del caso.
 * @param output         Salida esperada del caso.
 * @param expectedOutput Salida esperada para la entrada.
 * @param isSample       Indica si es un caso de ejemplo público.
 */
public record CreateTestCaseRequestDTO(
                int orderIndex,
                String input,
                String output,
                String expectedOutput,
                boolean isSample) {
}