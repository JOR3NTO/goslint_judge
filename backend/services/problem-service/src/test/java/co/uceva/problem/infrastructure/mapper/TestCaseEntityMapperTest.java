package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.TestCase;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.persistence.entity.TestCaseEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseEntityMapperTest {

    @Test
    void shouldMapDomainToEntityAndBack() {
        TestCase testCase = ProblemFixtures.aTestCase(UUID.randomUUID(), 3);
        testCase.setId(UUID.randomUUID());

        TestCaseEntity entity = TestCaseEntityMapper.toEntity(testCase);
        TestCase mapped = TestCaseEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(testCase.getId());
        assertThat(mapped.getProblemId()).isEqualTo(testCase.getProblemId());
        assertThat(mapped.getInput()).isEqualTo(testCase.getInput());
        assertThat(mapped.getOutput()).isEqualTo(testCase.getOutput());
        assertThat(mapped.getExpectedOutput()).isEqualTo(testCase.getExpectedOutput());
        assertThat(mapped.getOrderIndex()).isEqualTo(testCase.getOrderIndex());
        assertThat(mapped.isSample()).isEqualTo(testCase.isSample());
        assertThat(mapped.getCreatedAt()).isEqualTo(testCase.getCreatedAt());
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(TestCaseEntityMapper.toEntity(null)).isNull();
        assertThat(TestCaseEntityMapper.toDomain(null)).isNull();
    }
}
