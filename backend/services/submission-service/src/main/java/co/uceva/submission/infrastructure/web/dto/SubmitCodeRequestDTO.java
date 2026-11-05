package co.uceva.submission.infrastructure.web.dto;

import co.uceva.shared.domain.ProgrammingLanguage;

import java.util.UUID;

/**
 * DTO de solicitud para crear un nuevo envío de código fuente.
 *
 * @param teamId     Identificador del equipo que envía la solución.
 * @param problemId  Identificador del problema a resolver.
 * @param language   Lenguaje de programación del código fuente.
 * @param sourceCode Código fuente en texto plano.
 */
public record SubmitCodeRequestDTO(
        UUID teamId,
        UUID problemId,
        ProgrammingLanguage language,
        String sourceCode
) {}
