package co.uceva.submission.domain.model;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.VerdictStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionTest {

    @Test
    void shouldCreateSubmissionWithFactoryMethod() {
        UUID teamId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        Submission submission = Submission.create(teamId, problemId, ProgrammingLanguage.JAVA, "class Main {}");

        Instant now = Instant.now();
        assertThat(submission.getId()).isNotNull();
        assertThat(submission.getTeamId()).isEqualTo(teamId);
        assertThat(submission.getProblemId()).isEqualTo(problemId);
        assertThat(submission.getLanguage()).isEqualTo(ProgrammingLanguage.JAVA);
        assertThat(submission.getSourceCode()).isEqualTo("class Main {}");
        assertThat(submission.getVerdict()).isEqualTo(VerdictStatus.PENDING);
        assertThat(submission.getExecutionTimeMs()).isEqualTo(0);
        assertThat(submission.getMemoryUsedKb()).isEqualTo(0);
        assertThat(submission.getCodeSizeBytes()).isGreaterThan(0);
        assertThat(submission.getSubmittedAt()).isBetween(now.minusSeconds(1), now.plusSeconds(1));
    }

    @Test
    void shouldUpdateVerdict() {
        Submission submission = Submission.create(
                UUID.randomUUID(), UUID.randomUUID(), ProgrammingLanguage.PYTHON, "print(1)"
        );

        submission.updateVerdict(VerdictStatus.ACCEPTED, 120, 8192);

        assertThat(submission.getVerdict()).isEqualTo(VerdictStatus.ACCEPTED);
        assertThat(submission.getExecutionTimeMs()).isEqualTo(120);
        assertThat(submission.getMemoryUsedKb()).isEqualTo(8192);
    }

    @Test
    void shouldSetSourceCodeAndRecalculateSize() {
        Submission submission = Submission.create(
                UUID.randomUUID(), UUID.randomUUID(), ProgrammingLanguage.CPP, "int main(){}"
        );
        long originalSize = submission.getCodeSizeBytes();

        submission.setSourceCode("int main(){ return 0; }");

        assertThat(submission.getSourceCode()).isEqualTo("int main(){ return 0; }");
        assertThat(submission.getCodeSizeBytes()).isGreaterThan(originalSize);
    }
}
