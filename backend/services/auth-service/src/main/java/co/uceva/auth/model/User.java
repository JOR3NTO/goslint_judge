package co.uceva.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Usuario de la plataforma. Es a la vez el modelo de negocio y la entidad JPA
 * de la tabla {@code users}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false, columnDefinition = "varchar(255) default 'Usuario'")
    private String firstName;

    @Column(name = "last_name", nullable = false, columnDefinition = "varchar(255) default 'Prueba'")
    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt de la contraseña. Nunca se guarda ni se expone en texto plano. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Institución educativa a la que pertenece el usuario (opcional). */
    private String institution;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Crea un estudiante nuevo, activo y con la fecha de registro actual.
     *
     * @param passwordHash Contraseña ya cifrada.
     */
    public static User newStudent(String firstName, String lastName, String username,
                                  String email, String passwordHash, String institution) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(Role.STUDENT)
                .institution(institution)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
