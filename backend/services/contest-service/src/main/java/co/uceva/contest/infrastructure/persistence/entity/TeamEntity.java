package co.uceva.contest.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla de equipos junto con sus integrantes.
 * Es la representación persistente de {@link co.uceva.contest.domain.model.Team}.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "teams")
public class TeamEntity {
    /** Identificador único del equipo. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identificador del concurso al que pertenece el equipo. */
    @Column(name = "contest_id", nullable = false, updatable = false)
    private UUID contestId;

    /** Capitán del equipo; nulo en equipos anteriores a esta regla. */
    @Column(name = "captain_id", updatable = false)
    private UUID captainId;

    /** Nombre del equipo, único dentro del concurso. */
    @Column(name = "team_name", nullable = false)
    private String teamName;

    /** Puntos acumulados. */
    @Column(name = "score", nullable = false)
    private int score;

    /** Tiempo de penalización acumulado. */
    @Column(name = "penalty_time", nullable = false)
    private long penaltyTime;

    /** Fecha y hora de creación del equipo. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Integrantes del equipo. La columna {@code team_id} la escribe {@link MemberEntity};
     * aquí solo se usa para leer, y quitar un integrante de la lista lo elimina.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private List<MemberEntity> members;
}
