package co.uceva.problem.infrastructure.persistence.adapter;

import co.uceva.problem.AbstractIntegrationTest;
import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.domain.repository.TestCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "co.uceva.problem.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "co.uceva.problem.infrastructure.persistence.repository")
@Import(TestCaseRepositoryImpl.class)
class TestCaseRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private TestCaseRepository repository;

    @Test
    void shouldSaveAndFindTestCaseById() {
        UUID id = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();
        TestCase testCase = new TestCase(
                id, problemId, "3", 1, true, "1 2", "3", Instant.now()
        );

        repository.save(testCase);
        Optional<TestCase> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getProblemId()).isEqualTo(problemId);
        assertThat(found.get().getOrderIndex()).isEqualTo(1);
    }

    @Test
    void shouldFindAllByProblemIdOrderedByOrderIndex() {
        UUID problemId = UUID.randomUUID();
        TestCase first = aTestCase(problemId, 2);
        TestCase second = aTestCase(problemId, 1);
        repository.save(first);
        repository.save(second);
        repository.save(aTestCase(UUID.randomUUID(), 1));

        List<TestCase> found = repository.findAllByProblemId(problemId);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(found.get(1).getOrderIndex()).isEqualTo(2);
    }

    @Test
    void shouldUpdateOrderIndexes() {
        UUID problemId = UUID.randomUUID();
        TestCase first = aTestCase(problemId, 1);
        TestCase second = aTestCase(problemId, 2);
        repository.save(first);
        repository.save(second);

        repository.updateOrderIndexes(Map.of(
                first.getId(), 10,
                second.getId(), 5
        ));

        List<TestCase> found = repository.findAllByProblemId(problemId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getId()).isEqualTo(second.getId());
        assertThat(found.get(0).getOrderIndex()).isEqualTo(5);
        assertThat(found.get(1).getId()).isEqualTo(first.getId());
        assertThat(found.get(1).getOrderIndex()).isEqualTo(10);
    }

    @Test
    void shouldDoNothingWhenUpdatingEmptyOrderMap() {
        repository.updateOrderIndexes(Map.of());
    }

    @Test
    void shouldDeleteTestCaseById() {
        TestCase testCase = aTestCase(UUID.randomUUID(), 1);
        repository.save(testCase);

        repository.deleteById(testCase.getId());

        assertThat(repository.findById(testCase.getId())).isEmpty();
    }

    private TestCase aTestCase(UUID problemId, int orderIndex) {
        return new TestCase(
                UUID.randomUUID(), problemId, "3", orderIndex, true, "1 2", "3", Instant.now()
        );
    }
}
