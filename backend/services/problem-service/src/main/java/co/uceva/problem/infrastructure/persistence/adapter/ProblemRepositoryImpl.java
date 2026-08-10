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
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link ProblemRepository}.
 * Traduce entre entidades de dominio y entidades JPA, delegando la persistencia a Spring Data.
 */
@Component
public class ProblemRepositoryImpl implements ProblemRepository {

    private final SpringDataProblemRepository springDataRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param springDataRepository Repositorio de Spring Data para problemas.
     */
    public ProblemRepositoryImpl(SpringDataProblemRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /**
     * Guarda un problema convirtiéndolo a entidad JPA y retornando el dominio persistido.
     *
     * @param problem Entidad de dominio a guardar.
     * @return Problema persistido.
     */
    @Override
    public Problem save(Problem problem) {
        ProblemEntity entity = ProblemEntityMapper.toEntity(problem);
        ProblemEntity saved = springDataRepository.save(entity);
        return ProblemEntityMapper.toDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID problemId) {
        springDataRepository.deleteById(problemId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Problem> findById(UUID problemId) {
        return springDataRepository.findById(problemId)
                .map(ProblemEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<Problem> findAllByCreatedBy(UUID createdBy) {
        return springDataRepository.findByCreatedBy(createdBy).stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Problem> findAllByTitle(String title) {
        return springDataRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Problem> findAll() {
        return springDataRepository.findAll().stream()
                .map(ProblemEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
