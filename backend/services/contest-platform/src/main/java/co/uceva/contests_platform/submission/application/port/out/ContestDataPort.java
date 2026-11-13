package co.uceva.contests_platform.submission.application.port.out;

import java.util.Optional;
import java.util.UUID;

import co.uceva.contests_platform.submission.domain.service.ContestDataSource;

/**
 * Puerto de salida hacia el módulo {@code contest}, del que el módulo {@code submission}
 * obtiene únicamente datos de lectura (equipos, concursos y sus problemas).
 * <p>
 * Toda la lógica de validación vive en el módulo {@code submission}; este puerto solo
 * transporta datos. Un recurso inexistente se expresa con {@code Optional.empty()}
 * (o lista vacía), mientras que un fallo de comunicación se señala con
 * {@link co.uceva.contests_platform.submission.application.exception.ContestServiceUnavailableException}:
 * confundir ambos casos rechazaría envíos legítimos cuando la consulta
 * simplemente falle.
 * </p>
 */
public interface ContestDataPort extends ContestDataSource {

    /**
     * Traduce un problema del concurso al problema del catálogo, que es el que conoce el juez
     * para cargar los límites y los casos de prueba.
     *
     * @param contestId        Identificador del concurso.
     * @param contestProblemId Identificador del problema dentro del concurso ({@code contest_problems.id}).
     * @return Identificador del problema en el catálogo, o vacío si no pertenece al concurso.
     */
    Optional<UUID> findCatalogProblemId(UUID contestId, UUID contestProblemId);
}
