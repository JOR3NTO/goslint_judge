package co.uceva.submission.infrastructure.mapper;

import co.uceva.submission.domain.model.Submission;
import co.uceva.submission.infrastructure.persistence.entity.SubmissionEntity;

/**
 * Mapper encargado de convertir entre la entidad de dominio {@link Submission}
 * y la entidad JPA {@link SubmissionEntity}.
 */
public class SubmissionEntityMapper {

    /**
     * Convierte una entidad de dominio en su representación JPA.
     *
     * @param domain Entidad de dominio.
     * @return Entidad JPA o {@code null} si el dominio es {@code null}.
     */
    public static SubmissionEntity toEntity(Submission domain) {
        if (domain == null) return null;

        return SubmissionEntity.builder()
                .id(domain.getId())
                .teamId(domain.getTeamId())
                .problemId(domain.getProblemId())
                .language(domain.getLanguage())
                .sourceCode(domain.getSourceCode())
                .verdict(domain.getVerdict())
                .executionTimeMs(domain.getExecutionTimeMs())
                .memoryUsedKb(domain.getMemoryUsedKb())
                .codeSizeBytes(domain.getCodeSizeBytes())
                .submittedAt(domain.getSubmittedAt())
                .build();
    }

    /**
     * Convierte una entidad JPA en su entidad de dominio.
     *
     * @param entity Entidad JPA.
     * @return Entidad de dominio o {@code null} si la entidad es {@code null}.
     */
    public static Submission toDomain(SubmissionEntity entity) {
        if (entity == null) return null;

        return Submission.builder()
                .id(entity.getId())
                .teamId(entity.getTeamId())
                .problemId(entity.getProblemId())
                .language(entity.getLanguage())
                .sourceCode(entity.getSourceCode())
                .verdict(entity.getVerdict())
                .executionTimeMs(entity.getExecutionTimeMs())
                .memoryUsedKb(entity.getMemoryUsedKb())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}
