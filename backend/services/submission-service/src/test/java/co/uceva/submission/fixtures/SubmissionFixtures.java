package co.uceva.submission.fixtures;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.shared.domain.VerdictStatus;
import co.uceva.submission.application.usecase.SubmitCodeUseCase.SubmitCodeCommand;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.web.dto.SubmitCodeRequestDTO;

import java.time.Instant;
import java.util.UUID;

public final class SubmissionFixtures {

    private SubmissionFixtures() {
    }

    public static final UUID SUBMISSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID TEAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID PROBLEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final String SOURCE_CODE = "print('hello world')";

    public static Submission aSubmission() {
        return aSubmission(SUBMISSION_ID, VerdictStatus.PENDING);
    }

    public static Submission aSubmission(UUID id) {
        return aSubmission(id, VerdictStatus.PENDING);
    }

    public static Submission aSubmission(UUID id, VerdictStatus verdict) {
        return aSubmission(id, verdict, SubmissionStatus.PENDING);
    }

    public static Submission aSubmission(UUID id, SubmissionStatus status) {
        return aSubmission(id, VerdictStatus.PENDING, status);
    }

    public static Submission aSubmission(UUID id, VerdictStatus verdict, SubmissionStatus status) {
        return Submission.builder()
                .id(id)
                .teamId(TEAM_ID)
                .problemId(PROBLEM_ID)
                .language(ProgrammingLanguage.PYTHON)
                .sourceCode(SOURCE_CODE)
                .verdict(verdict)
                .status(status)
                .executionTimeMs(0)
                .memoryUsedKb(0)
                .submittedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    public static Submission aSubmission(UUID id, UUID teamId, UUID problemId) {
        return Submission.builder()
                .id(id)
                .teamId(teamId)
                .problemId(problemId)
                .language(ProgrammingLanguage.PYTHON)
                .sourceCode(SOURCE_CODE)
                .verdict(VerdictStatus.PENDING)
                .status(SubmissionStatus.PENDING)
                .executionTimeMs(0)
                .memoryUsedKb(0)
                .submittedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    public static SubmitCodeCommand submitCodeCommand() {
        return new SubmitCodeCommand(
                TEAM_ID,
                PROBLEM_ID,
                ProgrammingLanguage.PYTHON,
                SOURCE_CODE
        );
    }

    public static SubmitCodeRequestDTO submitCodeRequest() {
        return new SubmitCodeRequestDTO(
                TEAM_ID,
                PROBLEM_ID,
                ProgrammingLanguage.PYTHON,
                SOURCE_CODE
        );
    }
}
