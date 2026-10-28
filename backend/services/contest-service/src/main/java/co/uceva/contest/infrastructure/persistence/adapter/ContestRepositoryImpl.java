package co.uceva.contest.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.repository.ContestRepository;
import co.uceva.contest.infrastructure.mapper.ContestEntityMapper;
import co.uceva.contest.infrastructure.persistence.entity.ContestEntity;
import co.uceva.contest.infrastructure.persistence.repository.SpringDataContestRepository;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link ContestRepository}.
 * Traduce entre entidades de dominio y entidades JPA, delegando la persistencia a Spring Data.
 */
@Component
public class ContestRepositoryImpl implements ContestRepository {

    private final SpringDataContestRepository springDataRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param springDataRepository Repositorio de Spring Data para concursos.
     */
    public ContestRepositoryImpl(SpringDataContestRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /**
     * Guarda un concurso convirtiéndolo a entidad JPA y retornando el dominio persistido.
     *
     * @param contest Entidad de dominio a guardar.
     * @return Concurso persistido.
     */
    @Override
    public Contest save(Contest contest) {
        ContestEntity entity = ContestEntityMapper.toEntity(contest);
        ContestEntity saved = springDataRepository.save(entity);
        return ContestEntityMapper.toDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID contestId) {
        springDataRepository.deleteById(contestId);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Contest> findById(UUID contestId) {
        return springDataRepository.findById(contestId)
                .map(ContestEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<Contest> findAllByCreatedBy(UUID createdBy) {
        return springDataRepository.findByCreatedBy(createdBy).stream()
                .map(ContestEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Contest> findAllByTitle(String title) {
        return springDataRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(ContestEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Contest> findAll() {
        return springDataRepository.findAll().stream()
                .map(ContestEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
