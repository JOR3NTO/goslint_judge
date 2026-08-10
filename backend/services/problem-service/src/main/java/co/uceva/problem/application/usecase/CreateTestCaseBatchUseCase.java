package co.uceva.problem.application.usecase;

import java.util.List;
import java.util.UUID;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;

/**
 * Puerto de entrada para el caso de uso de creación masiva de casos de prueba.
 */
public interface CreateTestCaseBatchUseCase {
    /**
     * Ejecuta la creación de varios casos de prueba asociados a un problema.
     *
     * @param command Datos necesarios para crear los casos de prueba.
     * @return Lista de casos de prueba creados.
     */
    List<TestCase> execute(CreateTestCaseBatchCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para la creación masiva.
     *
     * @param problemId Identificador del problema asociado.
     * @param testCases Lista de comandos individuales de creación de casos de prueba.
     */
    record CreateTestCaseBatchCommand(
        UUID problemId,
        List<CreateTestCaseCommand> testCases
    ) {}
}
