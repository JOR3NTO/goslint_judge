package co.uceva.auth.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
public class UserJpaEntity {

    /** Llave primaria mapeada como UUID */
    @Id
    private UUID id;

    /** Nombre del usuario */
    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(255) default 'Usuario'")
    private String firstName;

    /** Apellido del usuario */
    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(255) default 'Prueba'")
    private String lastName;

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
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
