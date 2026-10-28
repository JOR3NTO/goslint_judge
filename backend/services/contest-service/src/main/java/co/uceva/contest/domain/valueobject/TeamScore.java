package co.uceva.contest.domain.valueobject;

import co.uceva.contest.domain.exception.InvalidTeamException;

/**
 * Value Object inmutable que encapsula el puntaje y el tiempo de penalización de un equipo.
 * Garantiza que los valores numéricos cumplan con las invariantes de negocio.
 */
public record TeamScore(int score, long penaltyTime) {

    /** Puntuación inicial en cero para equipos recién registrados. */
    public static final TeamScore ZERO = new TeamScore(0, 0L);

    /**
     * Constructor compacto que valida los invariantes del puntaje.
     *
     * @param score       Puntaje acumulado (problemas resueltos o puntos).
     * @param penaltyTime Tiempo de penalización acumulado.
     * @throws InvalidTeamException si alguno de los valores es negativo.
     */
    public TeamScore {
        if (score < 0) {
            throw new InvalidTeamException("El puntaje no puede ser negativo.");
        }
        if (penaltyTime < 0) {
            throw new InvalidTeamException("El tiempo de penalización no puede ser negativo.");
        }
    }

    /**
     * Factory method para crear un nuevo {@link TeamScore}.
     *
     * @param score       Puntaje acumulado.
     * @param penaltyTime Tiempo de penalización acumulado.
     * @return Una nueva instancia de {@link TeamScore}.
     */
    public static TeamScore of(int score, long penaltyTime) {
        return new TeamScore(score, penaltyTime);
    }

    /**
     * Retorna un nuevo {@link TeamScore} resultado de incrementar el puntaje y la penalización actuales.
     *
     * @param additionalPoints  Puntos adicionales a sumar.
     * @param additionalPenalty Penalización adicional a sumar.
     * @return Nueva instancia con la suma de los valores.
     */
    public TeamScore add(int additionalPoints, long additionalPenalty) {
        if (additionalPoints < 0 || additionalPenalty < 0) {
            throw new InvalidTeamException("Los puntos y la penalización adicionales deben ser no negativos.");
        }
        return new TeamScore(this.score + additionalPoints, this.penaltyTime + additionalPenalty);
    }
}
