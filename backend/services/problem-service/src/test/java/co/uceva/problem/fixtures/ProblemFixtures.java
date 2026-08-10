package co.uceva.problem.fixtures;

import co.uceva.problem.application.usecase.CreateProblemUseCase.CreateProblemCommand;
import co.uceva.problem.application.usecase.CreateTestCaseUseCase.CreateTestCaseCommand;
import co.uceva.problem.application.usecase.UpdateProblemUseCase.UpdateProblemCommand;
import co.uceva.problem.application.usecase.UpdateTestCaseUseCase.UpdateTestCaseCommand;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.infrastructure.web.dto.CreateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.CreateTestCaseRequestDTO;
import co.uceva.problem.infrastructure.web.dto.DeleteTestCaseBatchRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateProblemRequestDTO;
import co.uceva.problem.infrastructure.web.dto.UpdateTestCaseRequestDTO;

import java.util.List;
import java.util.UUID;

public final class ProblemFixtures {

    private ProblemFixtures() {
    }

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PROBLEM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID TEST_CASE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    public static Problem aProblem() {
        return aProblem(PROBLEM_ID, "Suma dos números");
    }

    public static Problem aProblem(UUID id, String title) {
        return new Problem(
                id,
                USER_ID,
                title,
                "Dados dos enteros, imprime su suma.",
                1000,
                65536,
                800,
                java.time.Instant.parse("2024-01-01T00:00:00Z"),
                "Dos enteros separados por espacio.",
                "Un entero con la suma."
        );
    }

    public static TestCase aTestCase() {
        return aTestCase(PROBLEM_ID, 1);
    }

    public static TestCase aTestCase(UUID problemId, int orderIndex) {
        return new TestCase(
                TEST_CASE_ID,
                problemId,
                "3",
                orderIndex,
                true,
                "1 2",
                "3",
                java.time.Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    public static CreateProblemCommand createProblemCommand() {
        return new CreateProblemCommand(
                USER_ID,
                "Suma dos números",
                "Dados dos enteros, imprime su suma.",
                1000,
                65536,
                800,
                "Dos enteros separados por espacio.",
                "Un entero con la suma."
        );
    }

    public static UpdateProblemCommand updateProblemCommand() {
        return new UpdateProblemCommand(
                PROBLEM_ID,
                "Resta dos números",
                "Dados dos enteros, imprime su resta.",
                2000,
                131072,
                1200,
                "Dos enteros.",
                "Un entero con la resta."
        );
    }

    public static CreateTestCaseCommand createTestCaseCommand() {
        return new CreateTestCaseCommand(
                PROBLEM_ID,
                "3",
                1,
                true,
                "1 2",
                "3"
        );
    }

    public static UpdateTestCaseCommand updateTestCaseCommand() {
        return new UpdateTestCaseCommand(
                TEST_CASE_ID,
                "5",
                2,
                false,
                "2 3",
                "5"
        );
    }

    public static CreateProblemRequestDTO createProblemRequest() {
        return new CreateProblemRequestDTO(
                USER_ID,
                "Suma dos números",
                "Dados dos enteros, imprime su suma.",
                1000,
                65536,
                800,
                "Dos enteros separados por espacio.",
                "Un entero con la suma."
        );
    }

    public static UpdateProblemRequestDTO updateProblemRequest() {
        return new UpdateProblemRequestDTO(
                "Resta dos números",
                "Dados dos enteros, imprime su resta.",
                2000,
                131072,
                1200,
                "Dos enteros.",
                "Un entero con la resta."
        );
    }

    public static CreateTestCaseRequestDTO createTestCaseRequest() {
        return new CreateTestCaseRequestDTO(
                1,
                "1 2",
                "3",
                "3",
                true
        );
    }

    public static UpdateTestCaseRequestDTO updateTestCaseRequest() {
        return new UpdateTestCaseRequestDTO(
                "2 3",
                2,
                "5",
                "5",
                false
        );
    }

    public static CreateTestCaseBatchRequestDTO createTestCaseBatchRequest() {
        return new CreateTestCaseBatchRequestDTO(List.of(
                new CreateTestCaseRequestDTO(1, "1 2", "3", "3", true),
                new CreateTestCaseRequestDTO(2, "3 4", "7", "7", false)
        ));
    }

    public static DeleteTestCaseBatchRequestDTO deleteTestCaseBatchRequest(List<UUID> testCaseIds) {
        return new DeleteTestCaseBatchRequestDTO(testCaseIds);
    }
}
