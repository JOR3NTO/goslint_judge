package co.uceva.judge.domain.model;

import java.util.UUID;

/**
 * Value Object que representa, dentro del dominio de {@code judge-service}, un
 * caso de prueba contra el que se ejecuta la solución enviada por un
 * estudiante.
 * <p>
 * Es una copia acotada al dominio del juez: transporta únicamente lo que el
 * sandbox necesita para ejecutar y comparar (entrada, salida esperada y orden
 * de ejecución), sin los atributos de gestión propios de {@code problem-service}
 * (como su visibilidad como caso de ejemplo).
 * </p>
 *
 * @param id             Identificador del caso de prueba en {@code problem-service}.
 * @param input          Entrada estándar que se envía a la solución.
 * @param expectedOutput Salida esperada contra la que se compara la producida por la solución.
 * @param orderIndex     Orden de ejecución del caso de prueba dentro del problema.
 */
public record TestCase(UUID id, String input, String expectedOutput, int orderIndex) {

    /**
     * Constructor compacto que valida los invariantes del dominio.
     *
     * @param id             Identificador del caso de prueba.
     * @param input          Entrada estándar del caso de prueba.
     * @param expectedOutput Salida esperada del caso de prueba.
     * @param orderIndex     Orden de ejecución del caso de prueba.
     */
    public TestCase {
        if (id == null) {
            throw new IllegalArgumentException("El identificador del caso de prueba es obligatorio.");
        }
        if (expectedOutput == null) {
            throw new IllegalArgumentException("La salida esperada del caso de prueba es obligatoria.");
        }
        if (orderIndex < 0) {
            throw new IllegalArgumentException("El orden de ejecución del caso de prueba no puede ser negativo.");
        }
    }
}
