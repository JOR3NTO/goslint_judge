package co.uceva.problem.infrastructure.web.mapper;

import co.uceva.problem.application.usecase.CreateProblemUseCase.CreateProblemCommand;
import co.uceva.problem.application.usecase.UpdateProblemUseCase.UpdateProblemCommand;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.ProblemResponseDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemWebMapperTest {

    @Test
    void shouldMapCreateRequestToCommand() {
        CreateProblemRequestDTO dto = ProblemFixtures.createProblemRequest();

        CreateProblemCommand command = ProblemWebMapper.toCommand(dto);

        assertThat(command.createdBy()).isEqualTo(dto.createdBy());
        assertThat(command.title()).isEqualTo(dto.title());
        assertThat(command.difficulty()).isEqualTo(dto.difficult());
    }

    @Test
    void shouldMapUpdateRequestToCommand() {
        UUID problemId = UUID.randomUUID();
        UpdateProblemRequestDTO dto = ProblemFixtures.updateProblemRequest();

        UpdateProblemCommand command = ProblemWebMapper.toCommand(problemId, dto);

        assertThat(command.problemId()).isEqualTo(problemId);
        assertThat(command.title()).isEqualTo(dto.title());
        assertThat(command.difficultyRating()).isEqualTo(dto.difficult());
    }

    @Test
    void shouldMapDomainToResponse() {
        Problem problem = ProblemFixtures.aProblem(UUID.randomUUID(), "Respuesta");

        ProblemResponseDTO response = ProblemWebMapper.toResponse(problem);

        assertThat(response.id()).isEqualTo(problem.getId());
        assertThat(response.title()).isEqualTo(problem.getTitle());
        assertThat(response.difficult()).isEqualTo(problem.getDifficult());
    }
}
