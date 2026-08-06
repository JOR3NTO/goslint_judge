package co.uceva.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA (Java Persistence API).
 * Representa exactamente la tabla "users" en la base de datos PostgreSQL.
 * Contiene anotaciones de mapeo relacional (@Entity, @Column, @Table).
 * No contiene reglas de negocio, es solo un contenedor de datos para Hibernate.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserJpaEntity {

    /** Llave primaria mapeada como UUID */
    @Id
    private UUID id;

    /** Nombre de usuario, no puede ser nulo y debe ser único */
    @Column(nullable = false, unique = true)
    private String username;

    /** Correo electrónico, no puede ser nulo y debe ser único */
    @Column(nullable = false, unique = true)
    private String email;

    /** Contraseña cifrada en base de datos */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Rol del usuario (persistido como String VARCHAR) */
    @Column(nullable = false)
    private String role;

    /** Institución del usuario */
    private String institution;

    /** Bandera para saber si la cuenta está activa */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** Fecha de creación del registro */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
