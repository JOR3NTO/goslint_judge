package co.uceva.problem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import co.uceva.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de actualización de un caso de prueba.
 */
public interface UpdateTestCaseUseCase {
    /**
     * Ejecuta la actualización de un caso de prueba existente.
     *
     * @param command Datos necesarios para actualizar el caso de prueba.
     * @return El caso de prueba actualizado.
     */
    TestCase execute(UpdateTestCaseCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para actualizar un caso de prueba.
     *
     * @param testCaseId     Identificador del caso de prueba a actualizar.
     * @param expectedOutput Nueva salida esperada.
     * @param orderIndex     Nuevo orden de ejecución.
     * @param isSample       Nuevo valor de visibilidad pública.
     * @param input          Nueva entrada.
     * @param output         Nueva salida esperada.
     */
    record UpdateTestCaseCommand(
            UUID testCaseId,
            String expectedOutput,
            int orderIndex,
            boolean isSample,
            String input,
            String output) {
    }
}
