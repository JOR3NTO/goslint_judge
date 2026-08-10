package co.uceva.problem.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de Dominio que representa un caso de prueba asociado a un problema.
 * Los casos de prueba definen la entrada y salida esperada para evaluar
 * soluciones enviadas por los estudiantes.
 */
@Getter
@Setter
@NoArgsConstructor
public class TestCase {
    /** Identificador único universal del caso de prueba. */
    private UUID id;
    /** Identificador del problema al que pertenece este caso de prueba. */
    private UUID problemId;
    /** Salida esperada para la entrada proporcionada. */
    private String expectedOutput;
    /** Índice que define el orden de ejecución del caso de prueba. */
    private int orderIndex;
    /** Indica si el caso de prueba es público (visible para los estudiantes). */
    private boolean isSample;
    /** Entrada del caso de prueba. */
    private String input;
    /** Salida esperada del caso de prueba (alias de expectedOutput). */
    private String output;
    /** Fecha y hora de creación del caso de prueba. */
    private Instant createdAt;

    /**
     * Constructor privado usado por Lombok Builder.
     *
     * @param id             Identificador del caso de prueba.
     * @param problemId      Identificador del problema asociado.
     * @param expectedOutput Salida esperada.
     * @param orderIndex     Orden de ejecución.
     * @param isSample       Si es un caso de ejemplo público.
     * @param input          Entrada del caso.
     * @param output         Salida esperada del caso.
     * @param createdAt      Fecha de creación.
     */
    @Builder
    private TestCase(UUID id, UUID problemId, String expectedOutput, int orderIndex, boolean isSample, String input,
            String output, Instant createdAt) {
        this.id = id;
        this.problemId = problemId;
        this.expectedOutput = expectedOutput;
        this.orderIndex = orderIndex;
        this.isSample = isSample;
        this.input = input;
        this.output = output;
        this.createdAt = createdAt;
    }

    /**
     * Factory method para crear un nuevo caso de prueba con valores por defecto.
     *
     * @param problemId      Identificador del problema asociado.
     * @param expectedOutput Salida esperada.
     * @param orderIndex     Orden de ejecución.
     * @param isSample       Si es un caso de ejemplo público.
     * @param input          Entrada del caso.
     * @param output         Salida esperada del caso.
     * @return Una instancia de {@link TestCase} lista para ser persistida.
     */
    public static TestCase create(UUID problemId, String expectedOutput, int orderIndex, boolean isSample, String input,
            String output){
        return TestCase.builder()
                .id(UUID.randomUUID()) // Genera un identificador único seguro
                .problemId(problemId)
                .expectedOutput(expectedOutput)
                .orderIndex(orderIndex)
                .isSample(isSample)
                .input(input)
                .output(output)
                .createdAt(Instant.now()) // Fecha y hora actual de creación
                .build();
    }

    /**
     * Actualiza los atributos editables del caso de prueba.
     *
     * @param expectedOutput Nueva salida esperada.
     * @param orderIndex     Nuevo orden de ejecución.
     * @param isSample       Nuevo valor de visibilidad pública.
     * @param input          Nueva entrada.
     * @param output         Nueva salida esperada.
     */
    public void update(String expectedOutput, int orderIndex, boolean isSample, String input,
            String output){
        this.expectedOutput = expectedOutput;
        this.orderIndex = orderIndex;
        this.isSample = isSample;
        this.input = input;
        this.output = output;
    }
}
