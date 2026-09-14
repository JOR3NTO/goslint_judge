package co.uceva.submission.application.usecase;

import co.uceva.submission.domain.model.Submission;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de consulta de todos los envíos
 * realizados por un equipo específico, sin importar el problema.
 */
public interface GetSubmissionsByTeamUseCase {

    /**
     * Recupera todos los envíos de un equipo.
     *
     * @param teamId Identificador del equipo.
     * @return Lista de envíos realizados por el equipo.
     */
    List<Submission> execute(UUID teamId);
}
