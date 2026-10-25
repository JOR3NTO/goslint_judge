package co.uceva.contest.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.exception.ClarificationAlreadyAnsweredException;
import co.uceva.contest.domain.exception.InvalidClarificationException;
import lombok.Builder;
import lombok.Getter;

/**
 * Entidad de Dominio que representa una aclaración: la pregunta que un equipo hace sobre un
 * problema del concurso y la respuesta de los organizadores. Es una clase pura de Java sin
 * dependencias de frameworks o bases de datos.
 * <p>
 * Nace {@link ClarificationStatus#PENDING} y pasa a {@link ClarificationStatus#ANSWERED}
 * una sola vez, mediante {@link #answer(String)}. Sin setters de campo ni constructor vacío.
 * </p>
 */
@Getter
public class Clarification {

    /** Longitud máxima de la pregunta y de la respuesta. */
    public static final int MAX_TEXT_LENGTH = 2000;

    /** Identificador único universal de la aclaración. */
    private final UUID id;
    /** Identificador del problema del concurso sobre el que se pregunta ({@code ContestProblem}). */
    private final UUID problemId;
    /** Identificador del equipo que pregunta. */
    private final UUID teamId;
    /** Pregunta del equipo. */
    private final String question;
    /** Respuesta de los organizadores; nula mientras esté pendiente. */
    private String answer;
    /** Estado de la aclaración. */
    private ClarificationStatus status;
    /** Fecha y hora en que se envió la pregunta. */
    private final Instant createdAt;
    /** Fecha y hora en que se respondió; nula mientras esté pendiente. */
    private Instant answeredAt;

    /**
     * Constructor privado utilizado por el Builder de Lombok.
     *
     * @param id         Identificador único; se genera uno si es nulo.
     * @param problemId  Identificador del problema del concurso.
     * @param teamId     Identificador del equipo.
     * @param question   Pregunta; no vacía y de hasta {@value #MAX_TEXT_LENGTH} caracteres.
     * @param answer     Respuesta; obligatoria si el estado es {@code ANSWERED} y nula si es {@code PENDING}.
     * @param status     Estado; {@code PENDING} si es nulo.
     * @param createdAt  Fecha de creación; la actual si es nula.
     * @param answeredAt Fecha de respuesta; la actual si está respondida y es nula.
     */
    @Builder
    private Clarification(UUID id, UUID problemId, UUID teamId, String question, String answer,
                          ClarificationStatus status, Instant createdAt, Instant answeredAt) {
        if (problemId == null) {
            throw new InvalidClarificationException("El identificador del problema es obligatorio.");
        }
        if (teamId == null) {
            throw new InvalidClarificationException("El identificador del equipo es obligatorio.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.problemId = problemId;
        this.teamId = teamId;
        this.question = validText(question, "La pregunta");
        this.status = status != null ? status : ClarificationStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        if (this.status == ClarificationStatus.ANSWERED) {
            this.answer = validText(answer, "La respuesta");
            this.answeredAt = answeredAt != null ? answeredAt : Instant.now();
        } else if (answer != null) {
            throw new InvalidClarificationException("Una aclaración pendiente no puede tener respuesta.");
        }
    }

    /**
     * Factory method para que un equipo envíe una pregunta sobre un problema.
     *
     * @param problemId Identificador del problema del concurso.
     * @param teamId    Identificador del equipo que pregunta.
     * @param question  Pregunta del equipo.
     * @return Una aclaración pendiente lista para ser persistida.
     */
    public static Clarification ask(UUID problemId, UUID teamId, String question) {
        return Clarification.builder()
                .problemId(problemId)
                .teamId(teamId)
                .question(question)
                .build();
    }

    /**
     * Responde la aclaración y la marca como {@link ClarificationStatus#ANSWERED}.
     *
     * @param text Respuesta; no vacía y de hasta {@value #MAX_TEXT_LENGTH} caracteres.
     * @throws ClarificationAlreadyAnsweredException si ya fue respondida.
     * @throws InvalidClarificationException         si la respuesta es inválida.
     */
    public void answer(String text) {
        if (isAnswered()) {
            throw new ClarificationAlreadyAnsweredException();
        }
        this.answer = validText(text, "La respuesta");
        this.status = ClarificationStatus.ANSWERED;
        this.answeredAt = Instant.now();
    }

    /**
     * Indica si la aclaración ya tiene respuesta.
     *
     * @return {@code true} si su estado es {@link ClarificationStatus#ANSWERED}.
     */
    public boolean isAnswered() {
        return this.status == ClarificationStatus.ANSWERED;
    }

    private static String validText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidClarificationException(label + " no puede estar vacía.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new InvalidClarificationException(label + " no puede superar " + MAX_TEXT_LENGTH + " caracteres.");
        }
        return trimmed;
    }
}
