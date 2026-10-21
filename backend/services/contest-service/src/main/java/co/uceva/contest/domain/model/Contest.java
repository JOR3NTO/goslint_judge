package co.uceva.contest.domain.model;

import java.time.Instant;
import java.util.UUID;

import co.uceva.contest.domain.valueobject.ContestPeriod;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad principal de Dominio que representa un Concurso (maratón) de programación
 * dentro de la plataforma. Es una clase pura de Java sin dependencias de
 * frameworks o bases de datos.
 */
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    /** Identificador único universal del concurso. */
    private UUID id;
    /** Identificador del usuario creador del concurso. */
    private UUID createdBy;
    /** Título descriptivo del concurso. */
    private String title;
    /** Descripción detallada del concurso. */
    private String description;
    /** Contraseña cifrada requerida para la inscripción de equipos al concurso. */
    private String passwordHash;
    /** Período de vigencia (fecha de inicio y fin) del concurso. */
    private ContestPeriod period;
    /** Estado actual del ciclo de vida del concurso. */
    private ContestStatus status;
    /** Fecha y hora de creación del registro del concurso. */
    private Instant createdAt;

    /**
     * Constructor privado usado por Lombok Builder.
     *
     * @param id           Identificador único del concurso.
     * @param createdBy    Identificador del usuario creador.
     * @param title        Título descriptivo del concurso.
     * @param description  Descripción del concurso.
     * @param passwordHash Contraseña cifrada de acceso para los equipos.
     * @param period       Período de fechas del concurso.
     * @param status       Estado del concurso.
     * @param createdAt    Fecha y hora de registro.
     */
    @Builder
    private Contest(UUID id, UUID createdBy, String title, String description, String passwordHash,
                    ContestPeriod period, ContestStatus status, Instant createdAt) {
        this.id = id;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.passwordHash = passwordHash;
        this.period = period;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Factory method para crear un nuevo concurso con valores por defecto.
     * Agrupa la lógica de inicialización, genera un identificador único,
     * valida las fechas del período,
     * establece el estado inicial en {@link ContestStatus#UPCOMING} y asigna la fecha actual.
     *
     * @param createdBy    Identificador del usuario creador.
     * @param title        Título del concurso.
     * @param description  Descripción del concurso.
     * @param passwordHash Contraseña ya cifrada.
     * @param startTime    Fecha y hora de inicio (debe ser igual o posterior a la actual).
     * @param endTime      Fecha y hora de finalización (debe ser posterior a la de inicio).
     * @return Una instancia de {@link Contest} lista para ser persistida.
     */
    public static Contest create(UUID createdBy, String title, String description,
                                 String passwordHash,
                                 Instant startTime, Instant endTime) {
        return Contest.builder()
                .id(UUID.randomUUID())
                .createdBy(createdBy)
                .title(title)
                .description(description)
                .passwordHash(passwordHash)
                .period(new ContestPeriod(startTime, endTime))
                .status(ContestStatus.UPCOMING)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Factory method para crear un nuevo concurso a partir de un período ya construido.
     *
     * @param createdBy    Identificador del usuario creador.
     * @param title        Título del concurso.
     * @param description  Descripción del concurso.
     * @param passwordHash Contraseña ya cifrada.
     * @param period       Value Object con el período de fechas.
     * @return Una instancia de {@link Contest} lista para ser persistida.
     */
    public static Contest create(UUID createdBy, String title, String description,
                                 String passwordHash, ContestPeriod period) {
        return Contest.builder()
                .id(UUID.randomUUID())
                .createdBy(createdBy)
                .title(title)
                .description(description)
                .passwordHash(passwordHash)
                .period(period)
                .status(ContestStatus.UPCOMING)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Actualiza los atributos editables del concurso.
     *
     * @param title        Nuevo título.
     * @param description  Nueva descripción.
     * @param passwordHash Nueva contraseña cifrada.
     * @param period       Nuevo período de fechas.
     * @param status       Nuevo estado del concurso.
     */
    public void update(String title, String description, String passwordHash,
                       ContestPeriod period, ContestStatus status) {
        this.title = title;
        this.description = description;
        this.passwordHash = passwordHash;
        this.period = period;
        this.status = status;
    }

    /**
     * Actualiza el período de fechas del concurso validando las invariantes temporales.
     *
     * @param startTime Nueva fecha de inicio.
     * @param endTime   Nueva fecha de finalización.
     */
    public void updatePeriod(Instant startTime, Instant endTime) {
        this.period = new ContestPeriod(startTime, endTime);
    }

    /** @return La fecha y hora de inicio del concurso. */
    public Instant getStartTime() {
        return period != null ? period.getStartTime() : null;
    }

    /** @return La fecha y hora de finalización del concurso. */
    public Instant getEndTime() {
        return period != null ? period.getEndTime() : null;
    }
}
