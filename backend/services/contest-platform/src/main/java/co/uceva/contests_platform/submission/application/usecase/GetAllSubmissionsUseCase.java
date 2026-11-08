package co.uceva.contests_platform.submission.application.usecase;

import co.uceva.contests_platform.submission.domain.model.Submission;

import java.util.List;

/**
 * Puerto de entrada para el caso de uso de consulta de todos los envíos.
 */
public interface GetAllSubmissionsUseCase {

    /**
     * Recupera todos los envíos registrados en el sistema.
     *
     * @return Lista de todos los envíos.
     */
    List<Submission> execute();
}
