package co.uceva.contest.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad de Dominio que representa a un integrante (miembro) dentro de un equipo
 * participante en un concurso de programación.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"teamId", "userId"})
@ToString
public class Member {

    /** Identificador único universal del registro de membresía. */
    private UUID id;
    /** Identificador del equipo al que pertenece el integrante. */
    private UUID teamId;
    /** Identificador del usuario (estudiante) que forma parte del equipo. */
    private UUID userId;
    /** Fecha y hora exacta en la que el integrante se unió al equipo. */
    private Instant registeredAt;

    /**
     * Constructor privado usado por Lombok Builder.
     *
     * @param id           Identificador único de la membresía.
     * @param teamId       Identificador del equipo.
     * @param userId       Identificador del usuario.
     * @param registeredAt Fecha y hora de registro.
     */
    @Builder
    private Member(UUID id, UUID teamId, UUID userId, Instant registeredAt) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.teamId = teamId;
        this.userId = userId;
        this.registeredAt = registeredAt != null ? registeredAt : Instant.now();
    }

    /**
     * Factory method para crear un nuevo integrante asignando automáticamente
     * un identificador único y la fecha actual.
     *
     * @param teamId Identificador del equipo.
     * @param userId Identificador del usuario.
     * @return Una instancia de {@link Member} lista para ser asociada o persistida.
     */
    public static Member of(UUID teamId, UUID userId) {
        return Member.builder()
                .id(UUID.randomUUID())
                .teamId(teamId)
                .userId(userId)
                .registeredAt(Instant.now())
                .build();
    }

    /**
     * Factory method para reconstruir una instancia existente de base de datos o almacenamiento.
     *
     * @param id           Identificador único.
     * @param teamId       Identificador del equipo.
     * @param userId       Identificador del usuario.
     * @param registeredAt Fecha y hora de registro original.
     * @return Instancia reconstruida de {@link Member}.
     */
    public static Member of(UUID id, UUID teamId, UUID userId, Instant registeredAt) {
        return Member.builder()
                .id(id)
                .teamId(teamId)
                .userId(userId)
                .registeredAt(registeredAt)
                .build();
    }
}
