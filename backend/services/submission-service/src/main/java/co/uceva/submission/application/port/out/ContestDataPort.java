package co.uceva.submission.application.port.out;

import co.uceva.submission.domain.service.ContestDataSource;

/**
 * Puerto de salida hacia {@code contest-service}, del que {@code submission-service}
 * obtiene únicamente datos de lectura (equipos, concursos y sus problemas).
 * <p>
 * Toda la lógica de validación vive en {@code submission-service}; este puerto solo
 * transporta datos. Un recurso inexistente se expresa con {@code Optional.empty()}
 * (o lista vacía), mientras que un fallo de comunicación se señala con
 * {@link co.uceva.submission.application.exception.ContestServiceUnavailableException}:
 * confundir ambos casos rechazaría envíos legítimos cuando {@code contest-service}
 * simplemente esté caído.
 * </p>
 */
public interface ContestDataPort extends ContestDataSource {
}
