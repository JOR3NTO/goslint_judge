package co.uceva.contest.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.uceva.contest.domain.model.Clarification;

/**
 * Puerto de salida (contrato de repositorio) para la entidad {@link Clarification}.
 * Define las operaciones de persistencia sin acoplarse a una tecnología específica.
 */
public interface ClarificationRepository {

    /**
     * Guarda una aclaración y retorna la instancia persistida.
     *
     * @param clarification Aclaración a persistir.
     * @return La aclaración persistida.
     */
    Clarification save(Clarification clarification);

    /**
     * Busca una aclaración por su identificador único.
     *
     * @param id Identificador de la aclaración.
     * @return {@link Optional} con la aclaración si existe, o vacío en caso contrario.
     */
    Optional<Clarification> findById(UUID id);

    /**
     * Recupera las aclaraciones de todos los problemas de un concurso, de la más reciente a la más antigua.
     * Las aclaraciones son públicas: la lista es la misma para todos los participantes.
     *
     * @param contestId Identificador del concurso.
     * @return Lista de aclaraciones del concurso.
     */
    List<Clarification> findAllByContestId(UUID contestId);
}
