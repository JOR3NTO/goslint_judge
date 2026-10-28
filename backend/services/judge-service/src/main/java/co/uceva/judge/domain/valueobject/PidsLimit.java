package co.uceva.judge.domain.valueobject;

/**
 * Value Object que representa el número máximo de procesos que una solución
 * puede crear de forma simultánea dentro del sandbox.
 * Garantiza que el valor se encuentre dentro de los rangos válidos del dominio.
 */
public record PidsLimit(long pids) {

    /**
     * Número mínimo de procesos permitidos. No es 1 porque el límite no lo consume
     * solo la solución: una JVM arranca sus propios hilos (GC, JIT, servicio) antes
     * de ejecutar una sola línea del estudiante y, por debajo de 15, muere con
     * {@code pthread_create failed (EAGAIN)} y el envío recibe RUNTIME_ERROR por un
     * límite de la plataforma. Medido con JDK 17: pico de 14 procesos.
     */
    public static final long MIN_PIDS = 15;
    /** Número máximo de procesos permitidos de forma simultánea. */
    public static final long MAX_PIDS = 64;
    /**
     * Valor por defecto utilizado cuando no se especifica un límite. Deja margen
     * sobre el mínimo porque el número de hilos de la JVM depende de las CPUs que
     * ve: con 16 CPUs el pico medido fue de 20 procesos, y en una máquina con más
     * núcleos G1 pide más. Con un margen estrecho el mismo envío pasa o falla según
     * la tirada, que es peor que un límite generoso.
     */
    public static final long DEFAULT_PIDS = 32;

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
