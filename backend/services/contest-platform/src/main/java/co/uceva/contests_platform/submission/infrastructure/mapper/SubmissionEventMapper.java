package co.uceva.contests_platform.submission.infrastructure.mapper;

import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.contests_platform.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Mapper encargado de convertir la entidad de dominio {@link Submission} en el
 * evento de integración {@link SubmissionReceivedEvent} que viaja por el sistema
 * de mensajería hacia {@code judge-service}.
 */
public class SubmissionEventMapper {

    /**
     * Convierte una entidad de dominio en el evento publicable.
     *
     * @param domain           Entidad de dominio.
     * @param catalogProblemId Problema del catálogo; el envío guarda el del concurso, pero el juez
     *                         carga límites y casos de prueba desde el catálogo.
     * @return Evento de integración o {@code null} si el dominio es {@code null}.
     */
    public static SubmissionReceivedEvent toEvent(Submission domain, UUID catalogProblemId) {
        if (domain == null) return null;

        return new SubmissionReceivedEvent(
                domain.getId(),
                domain.getTeamId(),
                catalogProblemId,
                domain.getLanguage(),
                domain.getSourceCode(),
                domain.getSubmittedAt()
        );
    }
}
