package co.uceva.problem.domain.valueobject;

/**
 * Value Object que representa el límite de memoria permitido para un problema.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record MemoryLimit(int kilobytes) {

    /** Límite mínimo de memoria en kilobytes (4 MB). */
    public static final int MIN_KB = 4_096;     // 4 MB
    /** Límite máximo de memoria en kilobytes (512 MB). */
    public static final int MAX_KB = 262_144; //  512 MB

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param kilobytes Límite de memoria en kilobytes.
     */
    public MemoryLimit {
        if (kilobytes < MIN_KB || kilobytes > MAX_KB) {
            throw new IllegalArgumentException(
                "El límite de memoria debe estar entre " + MIN_KB + " KB (" + MIN_KB / 1024 + " MB) y "
                 + MAX_KB + " KB (" + MAX_KB / 1024 + " MB)"
            );
        }
    }

    /** @return El límite de memoria convertido a megabytes. */
    public double toMegabytes() {
        return kilobytes / 1024.0;
    }
}
