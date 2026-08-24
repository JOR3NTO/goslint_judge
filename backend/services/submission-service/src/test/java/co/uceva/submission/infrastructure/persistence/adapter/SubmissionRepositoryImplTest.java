package co.uceva.submission.infrastructure.persistence.adapter;

import co.uceva.submission.AbstractIntegrationTest;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "co.uceva.submission.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "co.uceva.submission.infrastructure.persistence.repository")
@Import(SubmissionRepositoryImpl.class)
class SubmissionRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private SubmissionRepository repository;

    @Test
    void shouldSaveAndFindSubmissionById() {
        UUID id = UUID.randomUUID();
        Submission submission = aSubmission(id, UUID.randomUUID(), UUID.randomUUID(), "code");

        repository.save(submission);
        Optional<Submission> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getSourceCode()).isEqualTo("code");
    }

    @Test
    void shouldReturnEmptyWhenSubmissionNotFound() {
        Optional<Submission> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByProblemId() {
        UUID problemId = UUID.randomUUID();
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), problemId, "a"));
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), problemId, "b"));
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "c"));

        List<Submission> results = repository.findByProblemId(problemId);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByTeamId() {
        UUID teamId = UUID.randomUUID();
        repository.save(aSubmission(UUID.randomUUID(), teamId, UUID.randomUUID(), "a"));
        repository.save(aSubmission(UUID.randomUUID(), teamId, UUID.randomUUID(), "b"));

        List<Submission> results = repository.findByTeamId(teamId);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByProblemIdAndTeamId() {
        UUID problemId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        repository.save(aSubmission(UUID.randomUUID(), teamId, problemId, "a"));
        repository.save(aSubmission(UUID.randomUUID(), teamId, problemId, "b"));
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), problemId, "c"));

        List<Submission> results = repository.findByProblemIdAndTeamId(problemId, teamId);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindAll() {
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "a"));
        repository.save(aSubmission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "b"));

        List<Submission> results = repository.findAll();

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldDeleteById() {
        UUID id = UUID.randomUUID();
        repository.save(aSubmission(id, UUID.randomUUID(), UUID.randomUUID(), "x"));

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void shouldCheckExistenceByTeamIdAndProblemIdAndSourceCode() {
        UUID teamId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        String sourceCode = "print(1)";
        repository.save(aSubmission(UUID.randomUUID(), teamId, problemId, sourceCode));

        assertThat(repository.existsByTeamIdAndProblemIdAndSourceCode(teamId, problemId, sourceCode)).isTrue();
        assertThat(repository.existsByTeamIdAndProblemIdAndSourceCode(teamId, problemId, "other")).isFalse();
    }

    private Submission aSubmission(UUID id, UUID teamId, UUID problemId, String sourceCode) {
        return Submission.builder()
                .id(id)
                .teamId(teamId)
                .problemId(problemId)
                .language(co.uceva.shared.domain.ProgrammingLanguage.PYTHON)
                .sourceCode(sourceCode)
                .verdict(co.uceva.shared.domain.VerdictStatus.PENDING)
                .executionTimeMs(0)
                .memoryUsedKb(0)
                .submittedAt(Instant.now())
                .build();
    }
}
