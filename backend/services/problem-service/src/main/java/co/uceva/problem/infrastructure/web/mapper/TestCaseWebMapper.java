package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.application.usecase.UpdateTestCaseUseCase.UpdateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.TestCaseResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateTestCaseRequestDTO;

import java.util.UUID;

public class TestCaseWebMapper {

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
}