package co.uceva.problem.infrastructure.web.dto;

import java.util.List;

public record CreateTestCaseBatchRequestDTO(
        List<CreateTestCaseRequestDTO> testCases) {
}
