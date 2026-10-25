package co.uceva.contest.domain.service;

import java.util.UUID;

import co.uceva.contest.domain.exception.ProblemCatalogUnavailableException;

/**
 * Puerto de salida hacia el catálogo de problemas de la plataforma.
 * <p>
 * El dominio solo necesita saber si un problema existe, sin conocer cómo se consulta:
 * hoy es una llamada HTTP a {@code problem-service}, pero si la plataforma pasa a ser un
 * monolito basta con otro adaptador que use directamente el caso de uso de problemas.
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
