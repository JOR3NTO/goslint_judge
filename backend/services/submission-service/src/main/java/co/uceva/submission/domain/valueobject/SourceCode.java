package co.uceva.submission.domain.valueobject;

import co.uceva.shared.domain.ProgrammingLanguage;

/**
 * Value Object que representa el código fuente enviado por un estudiante.
 * <p>
 * Encapsula el contenido del código y el lenguaje en el que está escrito,
 * garantizando que se cumplan los invariantes de dominio: código no vacío,
 * lenguaje definido y tamaño dentro de los límites permitidos.
 * </p>
 */
public record SourceCode(String content, ProgrammingLanguage language) {

    /** Tamaño máximo permitido para el código fuente (128 KB). */
    public static final int MAX_SIZE_BYTES = 131_072;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param content  Código fuente en texto plano.
     * @param language Lenguaje de programación del código.
     */
    public SourceCode {
        if (language == null) {
            throw new IllegalArgumentException("El lenguaje de programación es obligatorio.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El código fuente no puede estar vacío.");
        }
        if (content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "El código fuente excede el tamaño máximo permitido de " + MAX_SIZE_BYTES + " bytes."
            );
        }
    }

    /**
     * Calcula el tamaño en bytes del código fuente.
     *
     * @return Cantidad de bytes que ocupa el contenido del código.
     */
    public long sizeInBytes() {
        return content.getBytes().length;
    }
}
