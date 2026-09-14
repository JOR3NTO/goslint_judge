package co.uceva.submission.infrastructure.mapper;

import co.uceva.shared.domain.event.SubmissionReceivedEvent;
import co.uceva.submission.domain.model.Submission;

/**
 * Mapper encargado de convertir la entidad de dominio {@link Submission} en el
 * evento de integración {@link SubmissionReceivedEvent} que viaja por el sistema
 * de mensajería hacia {@code judge-service}.
 */
public class SubmissionEventMapper {

    /**
     * Convierte una entidad de dominio en el evento publicable.
     *
     * @param domain Entidad de dominio.
     * @return Evento de integración o {@code null} si el dominio es {@code null}.
     */
    public static SubmissionReceivedEvent toEvent(Submission domain) {
        if (domain == null) return null;

        return new SubmissionReceivedEvent(
                domain.getId(),
                domain.getTeamId(),
                domain.getProblemId(),
                domain.getLanguage(),
                domain.getSourceCode(),
                domain.getSubmittedAt()
        );
    }
}
