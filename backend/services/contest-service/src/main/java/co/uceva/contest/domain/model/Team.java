package co.uceva.contest.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import co.uceva.contest.domain.exception.InvalidTeamException;
import co.uceva.contest.domain.exception.MaxTeamMembersReachedException;
import co.uceva.contest.domain.exception.MemberAlreadyInTeamException;
import co.uceva.contest.domain.exception.MemberNotFoundException;
import co.uceva.contest.domain.valueobject.TeamName;
import co.uceva.contest.domain.valueobject.TeamScore;
import lombok.Builder;
import lombok.Getter;

/**
 * Entidad principal de Dominio (Aggregate Root) que representa un Equipo participante
 * en un concurso (maratón) de programación.
 * <p>
 * Encapsula la identidad del equipo, su pertenencia al concurso, el nombre
 * ({@link TeamName}), el puntaje con su penalización ({@link TeamScore}) y la gestión
 * de sus miembros integrantes. Es una clase pura de Java sin dependencias de
 * frameworks o bases de datos.
 * </p>
 * <p>
 * Sin setters de campo ni constructor vacío: el estado solo cambia mediante métodos que
 * validan las invariantes, y una instancia solo se construye con {@link #builder()} o
 * los factory methods.
 * </p>
 */
@Getter
public class Team {

    /** Cantidad máxima predeterminada de integrantes por equipo en una maratón (regla estándar ICPC). */
    public static final int DEFAULT_MAX_MEMBERS = 3;

    /** Identificador único universal del equipo. */
    private final UUID id;
    /** Identificador del concurso al que pertenece el equipo. */
    private final UUID contestId;
    /** Capitán del equipo; es el único que puede invitar. Nulo solo en equipos anteriores a esta regla. */
    private final UUID captainId;
    /** Nombre representativo del equipo. */
    private TeamName teamName;
    /** Puntaje acumulado y tiempo de penalización del equipo. */
    private TeamScore teamScore;
    /** Fecha y hora de creación o inscripción del equipo. */
    private final Instant createdAt;
    /** Lista de miembros integrantes del equipo. */
    private final List<Member> members;

    /**
     * Constructor privado utilizado por el Builder de Lombok.
     *
     * @param id        Identificador único del equipo; se genera uno si es nulo.
     * @param contestId Identificador del concurso.
     * @param teamName  Nombre del equipo.
     * @param teamScore Puntaje y penalización; {@link TeamScore#ZERO} si es nulo.
     * @param createdAt Fecha y hora de creación; la actual si es nula.
     * @param members   Lista de integrantes iniciales.
     */
    @Builder
    private Team(UUID id, UUID contestId, UUID captainId, TeamName teamName, TeamScore teamScore,
                 Instant createdAt, List<Member> members) {
        if (contestId == null) {
            throw new InvalidTeamException("El identificador del concurso es obligatorio.");
        }
        if (teamName == null) {
            throw new InvalidTeamException("El nombre del equipo no puede ser nulo.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.contestId = contestId;
        this.captainId = captainId;
        this.teamName = teamName;
        this.teamScore = teamScore != null ? teamScore : TeamScore.ZERO;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    /**
     * Factory method para crear un nuevo equipo dentro de un concurso con puntaje en cero.
     *
     * @param contestId Identificador del concurso.
     * @param teamName  Value Object con el nombre del equipo.
     * @return Una instancia de {@link Team} lista para ser persistida.
     */
    public static Team create(UUID contestId, TeamName teamName) {
        return Team.builder()
                .contestId(contestId)
                .teamName(teamName)
                .build();
    }

    /**
     * Factory method para crear un nuevo equipo e incorporar a su usuario creador.
     *
     * @param contestId     Identificador del concurso.
     * @param teamName      Value Object con el nombre del equipo.
     * @param creatorUserId Identificador del usuario que crea e integra el equipo.
     * @return Una instancia de {@link Team} con el creador como primer integrante.
     */
    public static Team create(UUID contestId, TeamName teamName, UUID creatorUserId) {
        Team team = Team.builder()
                .contestId(contestId)
                .captainId(creatorUserId)
                .teamName(teamName)
                .build();
        team.addMember(creatorUserId);
        return team;
    }

    /**
     * Cambia el nombre del equipo.
     *
     * @param newName Nuevo nombre; no puede ser nulo.
     */
    public void updateName(TeamName newName) {
        if (newName == null) {
            throw new InvalidTeamException("El nombre del equipo no puede ser nulo.");
        }
        this.teamName = newName;
    }

    /**
     * Agrega un miembro al equipo respetando el límite por defecto de integrantes.
     *
     * @param userId Identificador del usuario.
     * @return La entidad {@link Member} creada y agregada.
     */
    public Member addMember(UUID userId) {
        return addMember(userId, DEFAULT_MAX_MEMBERS);
    }

    /**
     * Agrega un miembro al equipo con un límite máximo personalizado de integrantes.
     *
     * @param userId     Identificador del usuario.
     * @param maxMembers Cantidad máxima de integrantes permitidos.
     * @return La entidad {@link Member} creada y agregada.
     */
    public Member addMember(UUID userId, int maxMembers) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
        if (hasMember(userId)) {
            throw new MemberAlreadyInTeamException(userId);
        }
        if (this.members.size() >= maxMembers) {
            throw new MaxTeamMembersReachedException(maxMembers);
        }
        Member member = Member.of(this.id, userId);
        this.members.add(member);
        return member;
    }

    /**
     * Elimina a un integrante del equipo por su identificador de usuario.
     *
     * @param userId Identificador del usuario a eliminar.
     */
    public void removeMember(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio.");
        }
        boolean removed = this.members.removeIf(m -> userId.equals(m.getUserId()));
        if (!removed) {
            throw new MemberNotFoundException(userId, this.id);
        }
    }

    /**
     * Determina si el usuario especificado ya pertenece al equipo.
     *
     * @param userId Identificador del usuario a consultar.
     * @return {@code true} si el usuario es miembro del equipo; {@code false} en caso contrario.
     */
    public boolean isCaptain(UUID userId) {
        return userId != null && userId.equals(this.captainId);
    }

    public boolean isFull() {
        return this.members.size() >= DEFAULT_MAX_MEMBERS;
    }

    public boolean hasMember(UUID userId) {
        if (userId == null) {
            return false;
        }
        return this.members.stream().anyMatch(m -> userId.equals(m.getUserId()));
    }

    /**
     * Retorna la lista de identificadores de usuarios que pertenecen al equipo.
     * Útil para responder al contrato de notificación de envíos.
     *
     * @return Lista de {@link UUID} de los integrantes.
     */
    public List<UUID> getMemberUserIds() {
        return this.members.stream()
                .map(Member::getUserId)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Incrementa el puntaje y el tiempo de penalización acumulados por el equipo.
     *
     * @param points  Puntos adicionales ganados (ej. problema resuelto).
     * @param penalty Penalización adicional.
     */
    public void addScore(int points, long penalty) {
        this.teamScore = this.teamScore.add(points, penalty);
    }

    /**
     * Reemplaza el puntaje y la penalización acumulados del equipo.
     *
     * @param teamScore Value Object con los nuevos valores; no puede ser nulo.
     */
    public void updateScore(TeamScore teamScore) {
        if (teamScore == null) {
            throw new InvalidTeamException("El puntaje no puede ser nulo.");
        }
        this.teamScore = teamScore;
    }

    /**
     * Devuelve una vista inmutable de la lista de integrantes para proteger la invariante del agregado.
     *
     * @return Lista no modificable de integrantes.
     */
    public List<Member> getMembers() {
        return Collections.unmodifiableList(this.members);
    }
}
