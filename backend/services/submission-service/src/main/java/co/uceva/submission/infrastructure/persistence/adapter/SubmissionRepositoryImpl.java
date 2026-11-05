package co.uceva.submission.infrastructure.persistence.adapter;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.shared.domain.SubmissionStatus;
import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.domain.repository.SubmissionRepository;
import co.uceva.submission.infrastructure.mapper.SubmissionEntityMapper;
import co.uceva.submission.infrastructure.persistence.entity.SubmissionEntity;
import co.uceva.submission.infrastructure.persistence.repository.SpringDataSubmissionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de infraestructura que implementa el puerto de salida {@link SubmissionRepository}.
 * Traduce entre entidades de dominio y entidades JPA, delegando la persistencia a Spring Data.
 */
@Component
public class SubmissionRepositoryImpl implements SubmissionRepository {

    private final SpringDataSubmissionRepository springDataRepository;

    /**
     * Inyección de dependencias mediante constructor.
     *
     * @param springDataRepository Repositorio de Spring Data para envíos.
     */
    public SubmissionRepositoryImpl(SpringDataSubmissionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    /**
     * Guarda un envío convirtiéndolo a entidad JPA y retornando el dominio persistido.
     *
     * @param submission Entidad de dominio a guardar.
     * @return Envío persistido.
     */
    @Override
    public Submission save(Submission submission) {
        SubmissionEntity entity = SubmissionEntityMapper.toEntity(submission);
        SubmissionEntity saved = springDataRepository.save(entity);
        return SubmissionEntityMapper.toDomain(saved);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Submission> findById(UUID submissionId) {
        return springDataRepository.findById(submissionId)
                .map(SubmissionEntityMapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public List<Submission> findByProblemId(UUID problemId) {
        return springDataRepository.findByProblemId(problemId).stream()
                .map(SubmissionEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Submission> findByTeamId(UUID teamId) {
        return springDataRepository.findByTeamId(teamId).stream()
                .map(SubmissionEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Submission> findByProblemIdAndTeamId(UUID problemId, UUID teamId) {
        return springDataRepository.findByProblemIdAndTeamId(problemId, teamId).stream()
                .map(SubmissionEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public List<Submission> findAll() {
        return springDataRepository.findAll().stream()
                .map(SubmissionEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteById(UUID submissionId) {
        springDataRepository.deleteById(submissionId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
            UUID teamId, UUID problemId, String sourceCode, ProgrammingLanguage language) {
        return springDataRepository.existsByTeamIdAndProblemIdAndSourceCodeAndLanguage(
                teamId, problemId, sourceCode, language);
    }

    /** {@inheritDoc} */
    @Override
    public List<Submission> findStalePending(Instant submittedBefore, int limit) {
        return springDataRepository.findByStatusAndSubmittedAtBeforeOrderBySubmittedAtAsc(
                        SubmissionStatus.PENDING, submittedBefore, PageRequest.of(0, limit)).stream()
                .map(SubmissionEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
