package co.uceva.problem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import co.uceva.problem.domain.model.Problem;
import co.uceva.problem.domain.repository.ProblemRepository;
import co.uceva.problem.infrastructure.mapper.ProblemEntityMapper;
import co.uceva.problem.infrastructure.persistence.entity.ProblemEntity;
import co.uceva.problem.infrastructure.persistence.repository.SpringDataProblemRepository;

public class ProblemRepositoryImpl implements ProblemRepository {

    private final SpringDataProblemRepository springDataRepository;

    public ProblemRepositoryImpl(SpringDataProblemRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Problem save(Problem problem) {
        ProblemEntity entity = ProblemEntityMapper.toEntity(problem);
        ProblemEntity saved = springDataRepository.save(entity);
        return ProblemEntityMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID problemId) {
        springDataRepository.deleteById(problemId);
    }

    @Override
    public Optional<Problem> findById(UUID problemId) {
        return springDataRepository.findById(problemId)
                .map(ProblemEntityMapper::toDomain);
    }

    @Override
    public List<Problem> findAllByCreatedBy(UUID createdBy) {
        return springDataRepository.findByCreatedBy(createdBy).stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Problem> findAllByTitle(String title) {
        return springDataRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Problem> findAll() {
        return springDataRepository.findAll().stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
