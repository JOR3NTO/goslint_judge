package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de consulta del historial de envíos
 * de un equipo en un problema específico.
 */
public interface GetSubmissionHistoryUseCase {

    /**
     * Recupera los envíos realizados por un equipo en un problema específico.
     *
     * @param command Datos de filtro con el problema y el equipo.
     * @return Lista de envíos que coinciden con los filtros.
     */
    List<Submission> execute(GetSubmissionHistoryCommand command);

    /**
     * Comando inmutable que agrupa los filtros obligatorios para consultar
     * el historial de envíos.
     *
     * @param problemId Identificador del problema.
     * @param teamId    Identificador del equipo.
     */
    record GetSubmissionHistoryCommand(
            UUID problemId,
            UUID teamId
    ) {}
}
