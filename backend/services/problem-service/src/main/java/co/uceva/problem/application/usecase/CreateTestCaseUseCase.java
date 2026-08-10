package co.uceva.problem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de creación de un caso de prueba.
 */
public interface CreateTestCaseUseCase {
    /**
     * Ejecuta la creación de un nuevo caso de prueba.
     *
     * @param command Datos necesarios para crear el caso de prueba.
     * @return El caso de prueba creado.
     */
    TestCase execute(CreateTestCaseCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para crear un caso de prueba.
     *
     * @param problemId      Identificador del problema asociado.
     * @param expectedOutput Salida esperada.
     * @param orderIndex     Orden de ejecución.
     * @param isSample       Si es un caso de ejemplo público.
     * @param input          Entrada del caso.
     * @param output         Salida esperada del caso.
     */
    record CreateTestCaseCommand(
        UUID problemId,
        String expectedOutput,
        int orderIndex,
        boolean isSample,
        String input,
        String output
    ) {}
}
