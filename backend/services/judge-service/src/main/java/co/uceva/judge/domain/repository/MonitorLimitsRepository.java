package co.uceva.judge.domain.repository;

import co.uceva.judge.domain.model.MonitorLimits;

/**
 * Puerto de salida para los límites vigentes de los monitores del sandbox.
 */
public interface MonitorLimitsRepository {

    /** @return Los límites vigentes; nunca {@code null}. */
    MonitorLimits find();

    /**
     * Reemplaza los límites vigentes. Solo afecta a las evaluaciones que
     * empiecen después.
     *
     * @param limits Nuevos límites.
     */
    void save(MonitorLimits limits);
}
