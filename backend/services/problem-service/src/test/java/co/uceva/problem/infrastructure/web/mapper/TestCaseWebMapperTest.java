package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.application.usecase.UpdateTestCaseUseCase.UpdateTestCaseCommand;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.TestCaseResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateTestCaseRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseWebMapperTest {

    @Test
    void shouldMapCreateRequestToCommand() {
        UUID problemId = UUID.randomUUID();
        CreateTestCaseRequestDTO dto = ProblemFixtures.createTestCaseRequest();

        CreateTestCaseCommand command = TestCaseWebMapper.toCommand(problemId, dto);

        assertThat(command.problemId()).isEqualTo(problemId);
        assertThat(command.orderIndex()).isEqualTo(dto.orderIndex());
        assertThat(command.input()).isEqualTo(dto.input());
    }

    @Test
    void shouldMapUpdateRequestToCommand() {
        UUID testCaseId = UUID.randomUUID();
        UpdateTestCaseRequestDTO dto = ProblemFixtures.updateTestCaseRequest();

        UpdateTestCaseCommand command = TestCaseWebMapper.toCommand(testCaseId, dto);

        assertThat(command.testCaseId()).isEqualTo(testCaseId);
        assertThat(command.orderIndex()).isEqualTo(dto.orderIndex());
        assertThat(command.output()).isEqualTo(dto.output());
    }

    @Test
    void shouldMapDomainToResponse() {
        TestCase testCase = ProblemFixtures.aTestCase();
        testCase.setId(UUID.randomUUID());

        TestCaseResponseDTO response = TestCaseWebMapper.toResponse(testCase);

        assertThat(response.id()).isEqualTo(testCase.getId());
        assertThat(response.problemId()).isEqualTo(testCase.getProblemId());
        assertThat(response.orderIndex()).isEqualTo(testCase.getOrderIndex());
        assertThat(response.isSample()).isEqualTo(testCase.isSample());
    }
}
