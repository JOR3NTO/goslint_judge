package co.uceva.contest.domain.valueobject;

import co.uceva.contest.domain.exception.InvalidTeamException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value Object inmutable que representa el nombre de un equipo en un concurso.
 * Encapsula las reglas e invariantes de validación para el nombre del equipo.
 */
@Getter
@EqualsAndHashCode
public class TeamName {

    /** Longitud mínima permitida para el nombre de un equipo. */
    public static final int MIN_LENGTH = 3;
    /** Longitud máxima permitida para el nombre de un equipo. */
    public static final int MAX_LENGTH = 50;

    /** Valor textual normalizado del nombre del equipo. */
    private final String value;

    /**
     * Constructor principal que valida los invariantes del nombre de equipo.
     *
     * @param value Texto del nombre del equipo.
     * @throws InvalidTeamException si el nombre es nulo, vacío o fuera de rango.
     */
    public TeamName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidTeamException("El nombre del equipo no puede estar vacío.");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new InvalidTeamException(
                    "El nombre del equipo debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres."
            );
        }
        this.value = trimmed;
    }

    /**
     * Factory method para crear una instancia de {@link TeamName}.
     *
     * @param value Nombre textual del equipo.
     * @return Una nueva instancia de {@link TeamName}.
     */
    public static TeamName of(String value) {
        return new TeamName(value);
    }

    /**
     * Retorna el valor textual del nombre del equipo.
     *
     * @return Valor del nombre del equipo.
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
