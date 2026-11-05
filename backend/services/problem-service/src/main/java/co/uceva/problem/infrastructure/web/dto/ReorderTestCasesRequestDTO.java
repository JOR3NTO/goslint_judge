package co.uceva.problem.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de solicitud para reordenar los casos de prueba de un problema.
 *
 * @param testCaseIdsInOrder Lista de identificadores en el nuevo orden deseado.
 */
public record ReorderTestCasesRequestDTO(
        List<UUID> testCaseIdsInOrder
) {}