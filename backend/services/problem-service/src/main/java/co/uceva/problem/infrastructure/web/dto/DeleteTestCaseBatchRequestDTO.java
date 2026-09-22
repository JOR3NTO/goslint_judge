package co.uceva.problem.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de solicitud para eliminar varios casos de prueba de forma masiva.
 *
 * @param testCaseIds Lista de identificadores de los casos a eliminar.
 */
public record DeleteTestCaseBatchRequestDTO(
        List<UUID> testCaseIds) {
}
