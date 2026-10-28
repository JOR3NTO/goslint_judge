package co.uceva.submission.application.usecase;

import co.uceva.shared.domain.ProgrammingLanguage;
import co.uceva.submission.domain.model.Submission;

import java.util.UUID;

/**
 * Puerto de entrada para el caso de uso de recepción de un nuevo envío.
 * <p>
 * Orquesta la validación de reglas de negocio, la persistencia del envío
 * y la publicación del evento correspondiente para que el motor de
 * juzgamiento lo procese.
 * </p>
 */
public interface SubmitCodeUseCase {

    /**
     * Ejecuta el flujo de recepción de un nuevo envío de código fuente.
     *
     * @param command Datos necesarios para crear el envío.
     * @return El envío creado y persistido.
     */
    Submission execute(SubmitCodeCommand command);

    /**
     * Comando inmutable que agrupa los datos de entrada para crear un envío.
     *
     * @param teamId     Identificador del equipo que envía la solución.
     * @param problemId  Identificador del problema a resolver.
     * @param language   Lenguaje de programación del código fuente.
     * @param sourceCode Código fuente en texto plano.
     */
    record SubmitCodeCommand(
            UUID teamId,
            UUID problemId,
            ProgrammingLanguage language,
            String sourceCode
    ) {}
}
