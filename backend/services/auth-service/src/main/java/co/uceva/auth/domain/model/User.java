package co.uceva.auth.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal de Dominio que representa a un Usuario en el sistema.
 * Esta clase es pura de Java y no tiene dependencias de Spring Boot o base de datos.
 */
@Getter
@Setter
@Builder
public class User {
    /** Identificador único universal del usuario. */
    private UUID id;
    /** Nombre de usuario único (handle) usado en la plataforma. */
    private String username;
    /** Correo electrónico único del usuario. */
    private String email;
    /** Contraseña cifrada (hasheada) por seguridad. Nunca en texto plano. */
    private String passwordHash;
    /** Rol que define los permisos del usuario en la plataforma. */
    private Role role;
    /** Institución educativa a la que pertenece el usuario (opcional). */
    private String institution;
    /** Indica si la cuenta del usuario está activa o suspendida/eliminada. */
    private boolean isActive;
    /** Fecha y hora exacta en la que se creó la cuenta. */
    private LocalDateTime createdAt;
    
    /**
     * Factory method para crear un nuevo estudiante con valores por defecto.
     * Agrupa la lógica de inicialización para nuevos registros públicos.
     *
     * @param username     El nombre de usuario elegido.
     * @param email        El correo electrónico del usuario.
     * @param passwordHash La contraseña ya cifrada.
     * @param institution  La institución a la que pertenece.
     * @return Una instancia de {@link User} lista para ser persistida.
     */
    public static User createNewStudent(String username, String email, String passwordHash, String institution) {
        return User.builder()
                .id(UUID.randomUUID()) // Genera un ID seguro
                .username(username)
                .email(email)
                .passwordHash(passwordHash) // Aquí ya llega encriptada
                .role(Role.STUDENT) // Rol por defecto para visitantes que se registran
                .institution(institution)
                .isActive(true) // Cuenta activa desde el momento de la creación
                .createdAt(LocalDateTime.now()) // Fecha actual
                .build();
    }
}
