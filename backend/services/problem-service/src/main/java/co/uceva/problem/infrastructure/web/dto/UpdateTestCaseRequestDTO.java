package co.uceva.problem.infrastructure.web.dto;

/**
 * DTO de solicitud para actualizar un caso de prueba existente.
 *
 * @param input          Nueva entrada.
 * @param orderIndex     Nuevo orden de ejecución.
 * @param output         Nueva salida esperada.
 * @param expectedOutput Nueva salida esperada para la entrada.
 * @param isSample       Nuevo valor de visibilidad pública.
 */
public record UpdateTestCaseRequestDTO(
        String input,
        int orderIndex,
        String output,
        String expectedOutput,
        boolean isSample
) {}