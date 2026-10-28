package co.uceva.contest.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.uceva.contest.domain.model.ContestProblem;
import co.uceva.contest.domain.repository.ContestProblemRepository;
import co.uceva.contest.infrastructure.mapper.ContestProblemEntityMapper;
import co.uceva.contest.infrastructure.persistence.repository.SpringDataContestProblemRepository;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link ContestProblemRepository}.
 */
@Component
public class ContestProblemRepositoryImpl implements ContestProblemRepository {

    private final SpringDataContestProblemRepository springDataRepository;

    public ContestProblemRepositoryImpl(SpringDataContestProblemRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public ContestProblem save(ContestProblem contestProblem) {
        return ContestProblemEntityMapper.toDomain(
                springDataRepository.save(ContestProblemEntityMapper.toEntity(contestProblem)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ContestProblem> findById(UUID id) {
        return springDataRepository.findById(id).map(ContestProblemEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<ContestProblem> findAllByContestIdOrderByOrderIndexAsc(UUID contestId) {
        return springDataRepository.findByContestIdOrderByOrderIndexAsc(contestId).stream()
                .map(ContestProblemEntityMapper::toDomain).toList();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID id) {
        springDataRepository.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByContestIdAndProblemId(UUID contestId, UUID problemId) {
        return springDataRepository.existsByContestIdAndProblemId(contestId, problemId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByContestIdAndAlias(UUID contestId, String alias) {
        return springDataRepository.existsByContestIdAndAlias(contestId, alias);
    }
}
