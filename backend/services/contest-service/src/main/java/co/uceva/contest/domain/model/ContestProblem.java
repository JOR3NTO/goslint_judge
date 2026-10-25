package co.uceva.contest.domain.model;

import java.util.Locale;
import java.util.UUID;

import co.uceva.contest.domain.exception.InvalidContestProblemException;
import co.uceva.contest.domain.valueobject.BalloonColor;
import lombok.Builder;
import lombok.Getter;

/**
 * Entidad de Dominio que representa la asignación de un problema (de problem-service)
 * a un concurso, con su alias visible (A, B, C...), su posición, sus puntos y el
 * color del globo que recibe el equipo al resolverlo.
 * <p>
 * Sin setters de campo ni constructor vacío: el estado solo cambia mediante
 * {@link #update(String, int, int, BalloonColor)}, que valida las invariantes.
 * </p>
 */
@Getter
public class ContestProblem {

    /** Puntos predeterminados por problema resuelto (regla estándar ICPC). */
    public static final int DEFAULT_POINTS = 1;
    /** Longitud máxima del alias del problema. */
    public static final int MAX_ALIAS_LENGTH = 10;

    /** Identificador único universal de la asignación. */
    private final UUID id;
    /** Identificador del concurso. */
    private final UUID contestId;
    /** Identificador del problema en problem-service. */
    private final UUID problemId;
    /** Alias visible del problema dentro del concurso, en mayúsculas (ej. "A"). */
    private String alias;
    /** Posición del problema en el listado del concurso, desde 0. */
    private int orderIndex;
    /** Puntos que otorga resolver el problema. */
    private int points;
    /** Color del globo asociado al problema. */
    private BalloonColor balloonColor;

    /**
     * Constructor privado utilizado por el Builder de Lombok.
     *
     * @param id           Identificador único; se genera uno si es nulo.
     * @param contestId    Identificador del concurso.
     * @param problemId    Identificador del problema.
     * @param alias        Alias visible del problema.
     * @param orderIndex   Posición del problema (no negativa).
     * @param points       Puntos del problema (mayor que cero).
     * @param balloonColor Color del globo.
     */
    @Builder
    private ContestProblem(UUID id, UUID contestId, UUID problemId, String alias, int orderIndex,
                           int points, BalloonColor balloonColor) {
        if (contestId == null) {
            throw new InvalidContestProblemException("El identificador del concurso es obligatorio.");
        }
        if (problemId == null) {
            throw new InvalidContestProblemException("El identificador del problema es obligatorio.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.contestId = contestId;
        this.problemId = problemId;
        update(alias, orderIndex, points, balloonColor);
    }

    /**
     * Factory method para asignar un problema a un concurso con los puntos por defecto.
     *
     * @param contestId    Identificador del concurso.
     * @param problemId    Identificador del problema.
     * @param alias        Alias visible del problema.
     * @param orderIndex   Posición del problema.
     * @param balloonColor Color del globo.
     * @return Una instancia de {@link ContestProblem} lista para ser persistida.
     */
    public static ContestProblem create(UUID contestId, UUID problemId, String alias, int orderIndex,
                                        BalloonColor balloonColor) {
        return ContestProblem.builder()
                .contestId(contestId)
                .problemId(problemId)
                .alias(alias)
                .orderIndex(orderIndex)
                .points(DEFAULT_POINTS)
                .balloonColor(balloonColor)
                .build();
    }

    /**
     * Actualiza los atributos editables del problema dentro del concurso.
     *
     * @param alias        Alias visible; no vacío y de hasta {@value #MAX_ALIAS_LENGTH} caracteres.
     * @param orderIndex   Posición; no negativa.
     * @param points       Puntos; mayores que cero.
     * @param balloonColor Color del globo; no nulo.
     */
    public void update(String alias, int orderIndex, int points, BalloonColor balloonColor) {
        if (alias == null || alias.isBlank()) {
            throw new InvalidContestProblemException("El alias del problema no puede estar vacío.");
        }
        String normalizedAlias = alias.trim().toUpperCase(Locale.ROOT);
        if (normalizedAlias.length() > MAX_ALIAS_LENGTH) {
            throw new InvalidContestProblemException(
                    "El alias del problema no puede superar " + MAX_ALIAS_LENGTH + " caracteres.");
        }
        if (orderIndex < 0) {
            throw new InvalidContestProblemException("La posición del problema no puede ser negativa.");
        }
        if (points <= 0) {
            throw new InvalidContestProblemException("Los puntos del problema deben ser mayores que cero.");
        }
        if (balloonColor == null) {
            throw new InvalidContestProblemException("El color del globo es obligatorio.");
        }
        this.alias = normalizedAlias;
        this.orderIndex = orderIndex;
        this.points = points;
        this.balloonColor = balloonColor;
    }
}
