package co.uceva.problem.infrastructure.mapper;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.fixtures.ProblemFixtures;
import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemEntityMapperTest {

    @Test
    void shouldMapDomainToEntityAndBack() {
        Problem problem = ProblemFixtures.aProblem(UUID.randomUUID(), "Título");

        ProblemEntity entity = ProblemEntityMapper.toEntity(problem);
        Problem mapped = ProblemEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(problem.getId());
        assertThat(mapped.getCreatedBy()).isEqualTo(problem.getCreatedBy());
        assertThat(mapped.getTitle()).isEqualTo(problem.getTitle());
        assertThat(mapped.getStatement()).isEqualTo(problem.getStatement());
        assertThat(mapped.getTimeLimitMs()).isEqualTo(problem.getTimeLimitMs());
        assertThat(mapped.getMemoryLimitKb()).isEqualTo(problem.getMemoryLimitKb());
        assertThat(mapped.getDifficult()).isEqualTo(problem.getDifficult());
        assertThat(mapped.getCreatedAt()).isEqualTo(problem.getCreatedAt());
        assertThat(mapped.getInputFormat()).isEqualTo(problem.getInputFormat());
        assertThat(mapped.getOutputFormat()).isEqualTo(problem.getOutputFormat());
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(ProblemEntityMapper.toEntity(null)).isNull();
        assertThat(ProblemEntityMapper.toDomain(null)).isNull();
    }
}
