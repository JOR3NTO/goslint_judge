package co.uceva.problem.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record ReorderTestCasesRequestDTO(
        List<UUID> testCaseIdsInOrder
) {}