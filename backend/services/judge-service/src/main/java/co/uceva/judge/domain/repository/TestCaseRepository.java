package co.uceva.judge.domain.repository;

import java.util.List;
import java.util.UUID;

import co.uceva.judge.domain.model.TestCase;

/**
 * Puerto de salida (contrato de repositorio) de solo lectura para los casos de
 * prueba que {@code judge-service} necesita ejecutar.
 * <p>
 * {@code judge-service} no es dueño de estos datos: pertenecen a
 * {@code problem-service}. El contrato oculta cómo se obtienen (HTTP, caché,
 * etc.) sin acoplar el dominio a ninguna tecnología.
 * </p>
 */
public interface TestCaseRepository {

    /**
     * Recupera todos los casos de prueba de un problema, incluidos los privados,
     * ordenados por su orden de ejecución.
     *
     * @param problemId Identificador del problema.
     * @return Lista de casos de prueba en orden de ejecución; nunca {@code null}.
     * @throws co.uceva.judge.domain.exception.TestCasesNotFoundException si el problema no tiene casos de prueba.
     */
    List<TestCase> findAllByProblemId(UUID problemId);
}
