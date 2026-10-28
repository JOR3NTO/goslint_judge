package co.uceva.contest.infrastructure.mapper;

import co.uceva.contest.domain.model.Contest;
import co.uceva.contest.domain.valueobject.ContestPeriod;
import co.uceva.contest.infrastructure.persistence.entity.ContestEntity;

/**
 * Mapper encargado de convertir entre la entidad de dominio {@link Contest}
 * y la entidad JPA {@link ContestEntity}.
 */
public class ContestEntityMapper {

    /**
     * Convierte una entidad de dominio en su representación JPA.
     *
     * @param domain Entidad de dominio.
     * @return Entidad JPA o null si el dominio es null.
     */
    public static ContestEntity toEntity(Contest domain) {
        if (domain == null) return null;

        return ContestEntity.builder()
                .id(domain.getId())
                .createdBy(domain.getCreatedBy())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .passwordHash(domain.getPasswordHash())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * Convierte una entidad JPA en su entidad de dominio. El período se restaura
     * sin exigir fecha futura, porque el concurso puede haber iniciado ya.
     *
     * @param entity Entidad JPA.
     * @return Entidad de dominio o null si la entidad es null.
     */
    public static Contest toDomain(ContestEntity entity) {
        if (entity == null) return null;

        return Contest.builder()
                .id(entity.getId())
                .createdBy(entity.getCreatedBy())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .passwordHash(entity.getPasswordHash())
                .period(ContestPeriod.restore(entity.getStartTime(), entity.getEndTime()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
