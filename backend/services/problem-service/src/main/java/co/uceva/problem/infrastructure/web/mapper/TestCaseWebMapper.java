package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateTestCaseBatchUseCase;
import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.application.usecase.UpdateTestCaseUseCase.UpdateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.TestCaseResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateTestCaseRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Mapper encargado de convertir entre DTOs de la capa web y comandos/entidades
 * de la capa de aplicación y dominio para los casos de prueba.
 */
public class TestCaseWebMapper {

    /**
     * Convierte un DTO de creación y el identificador del problema en un comando de aplicación.
     *
     * @param problemId Identificador del problema asociado.
     * @param request   DTO con los datos de creación.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static CreateTestCaseCommand toCommand(UUID problemId, CreateTestCaseRequestDTO request) {
        return new CreateTestCaseCommand(
                problemId,
                request.expectedOutput(),
                request.orderIndex(),
                request.isSample(),
                request.input(),
                request.output()
        );
    }

    /**
     * Convierte un DTO de actualización y el identificador del caso de prueba en un comando de aplicación.
     *
     * @param testCaseId Identificador del caso de prueba a actualizar.
     * @param request    DTO con los datos de actualización.
     * @return Comando listo para ser ejecutado por el caso de uso.
     */
    public static UpdateTestCaseCommand toCommand(UUID testCaseId, UpdateTestCaseRequestDTO request) {
        return new UpdateTestCaseCommand(
                testCaseId,
                request.expectedOutput(),
                request.orderIndex(),
                request.isSample(),
                request.input(),
                request.output()
        );
    }

    /**
     * Convierte una entidad de dominio en un DTO de respuesta.
     *
     * @param domain Entidad {@link TestCase}.
     * @return DTO con los datos expuestos al cliente.
     */
    public static TestCaseResponseDTO toResponse(TestCase domain) {
        return new TestCaseResponseDTO(
                domain.getId(),
                domain.getProblemId(),
                domain.getInput(),
                domain.getOutput(),
                domain.getExpectedOutput(),
                domain.getOrderIndex(),
                domain.isSample(),
                domain.getCreatedAt()
        );
    }

    /**
     * Convierte un DTO de creación masiva en un comando de aplicación.
     *
     * @param problemId Identificador del problema asociado.
     * @param request   DTO con la lista de casos de prueba a crear.
     * @return Comando de creación masiva listo para ser ejecutado.
     */
    public static CreateTestCaseBatchUseCase.CreateTestCaseBatchCommand toCreateBatchCommand(UUID problemId, CreateTestCaseBatchRequestDTO request) {
        // Convertir cada DTO individual en su comando correspondiente
        List<CreateTestCaseCommand> commands = request.testCases().stream()
                .map(tc -> toCommand(problemId, tc))
                .toList();
        return new CreateTestCaseBatchUseCase.CreateTestCaseBatchCommand(problemId, commands);
    }
}