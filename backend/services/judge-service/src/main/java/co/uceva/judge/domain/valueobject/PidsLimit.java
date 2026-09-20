package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el número máximo de procesos que una solución
 * puede crear de forma simultánea dentro del sandbox.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record PidsLimit(long pids) {

    /** Número mínimo de procesos permitidos (al menos el proceso de la propia solución). */
    public static final long MIN_PIDS = 1;
    /** Número máximo de procesos permitidos de forma simultánea. */
    public static final long MAX_PIDS = 64;
    /** Valor por defecto utilizado cuando no se especifica un límite. */
    public static final long DEFAULT_PIDS = 5;

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param pids Número máximo de procesos permitidos.
     */
    public PidsLimit {
        if (pids < MIN_PIDS || pids > MAX_PIDS) {
            throw new IllegalArgumentException(
                "El límite de procesos debe estar entre " + MIN_PIDS + " y " + MAX_PIDS
            );
        }
    }

    /** @return El límite de procesos por defecto. */
    public static PidsLimit ofDefault() {
        return new PidsLimit(DEFAULT_PIDS);
    }
}
