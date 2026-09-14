package co.uceva.problem.infrastructure.web.dto;

import java.util.List;

/**
 * DTO de solicitud para crear varios casos de prueba de forma masiva.
 *
 * @param testCases Lista de casos de prueba a crear.
 */
public record CreateTestCaseBatchRequestDTO(
        List<CreateTestCaseRequestDTO> testCases) {
}
