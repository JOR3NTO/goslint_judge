package co.uceva.problem.infrastructure.persistence.adapter;

import co.uceva.problem.AbstractIntegrationTest;
import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
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
@EntityScan(basePackages = "co.uceva.problem.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "co.uceva.problem.infrastructure.persistence.repository")
@Import(ProblemRepositoryImpl.class)
class ProblemRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private ProblemRepository repository;

    @Test
    void shouldSaveAndFindProblemById() {
        UUID id = UUID.randomUUID();
        Problem problem = new Problem(
                id, UUID.randomUUID(), "Suma", "Statement",
                1000, 65536, 800, Instant.now(), "input", "output"
        );

        repository.save(problem);
        Optional<Problem> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Suma");
    }

    @Test
    void shouldReturnEmptyWhenProblemNotFound() {
        Optional<Problem> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllProblems() {
        repository.save(aProblem("Problema A"));
        repository.save(aProblem("Problema B"));

        List<Problem> problems = repository.findAll();

        assertThat(problems).hasSize(2);
    }

    @Test
    void shouldFindByCreatedBy() {
        UUID createdBy = UUID.randomUUID();
        repository.save(aProblem(createdBy, "Problema 1"));
        repository.save(aProblem(createdBy, "Problema 2"));
        repository.save(aProblem(UUID.randomUUID(), "Problema 3"));

        List<Problem> problems = repository.findAllByCreatedBy(createdBy);

        assertThat(problems).hasSize(2);
    }

    @Test
    void shouldFindByTitleContainingIgnoreCase() {
        repository.save(aProblem("Suma de enteros"));
        repository.save(aProblem("suma de matrices"));
        repository.save(aProblem("Resta de enteros"));

        List<Problem> problems = repository.findAllByTitle("suma");

        assertThat(problems).hasSize(2);
    }

    @Test
    void shouldDeleteProblemById() {
        Problem problem = aProblem("Por borrar");
        repository.save(problem);

        repository.deleteById(problem.getId());

        assertThat(repository.findById(problem.getId())).isEmpty();
    }

    private Problem aProblem(String title) {
        return aProblem(UUID.randomUUID(), title);
    }

    private Problem aProblem(UUID createdBy, String title) {
        return new Problem(
                UUID.randomUUID(), createdBy, title, "Statement",
                1000, 65536, 800, Instant.now(), "input", "output"
        );
    }
}
