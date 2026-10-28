package co.uceva.contest.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.uceva.contest.domain.model.Clarification;
import co.uceva.contest.domain.repository.ClarificationRepository;
import co.uceva.contest.infrastructure.mapper.ClarificationEntityMapper;
import co.uceva.contest.infrastructure.persistence.repository.SpringDataClarificationRepository;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link ClarificationRepository}.
 */
@Component
public class ClarificationRepositoryImpl implements ClarificationRepository {

    private final SpringDataClarificationRepository springDataRepository;

    public ClarificationRepositoryImpl(SpringDataClarificationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public Clarification save(Clarification clarification) {
        return ClarificationEntityMapper.toDomain(
                springDataRepository.save(ClarificationEntityMapper.toEntity(clarification)));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Clarification> findById(UUID id) {
        return springDataRepository.findById(id).map(ClarificationEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<Clarification> findAllByContestId(UUID contestId) {
        return springDataRepository.findByContestId(contestId).stream()
                .map(ClarificationEntityMapper::toDomain).toList();
    }
}
