package co.uceva.contests_platform.contest.domain.service;

import java.util.UUID;

import co.uceva.contests_platform.contest.domain.exception.ProblemCatalogUnavailableException;

/**
 * Puerto de salida hacia el catálogo de problemas de la plataforma.
 * <p>
 * El dominio solo necesita saber si un problema existe, sin conocer cómo se consulta:
 * hoy es una llamada directa al caso de uso del módulo {@code problem}, pero un adaptador
 * distinto podría consultarlo por red sin tocar el dominio.
 * </p>
 */
public interface ProblemCatalog {

    /**
     * Verifica si un problema existe en el catálogo.
     *
     * @param problemId Identificador del problema.
     * @return {@code true} si el problema existe; {@code false} en caso contrario.
     * @throws ProblemCatalogUnavailableException si no se pudo consultar el catálogo.
     */
    boolean exists(UUID problemId);
}
