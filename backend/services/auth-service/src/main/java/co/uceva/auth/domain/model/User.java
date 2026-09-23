package co.uceva.auth.domain.model;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal de Dominio que representa a un Usuario en el sistema.
 * Esta clase es pura de Java y no tiene dependencias de Spring Boot o base de datos.
 */
public class User {
    /** Identificador único universal del usuario. */
    private UUID id;
    /** Nombre del usuario. */
    private String firstName;
    /** Apellido del usuario. */
    private String lastName;
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
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private UUID id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String passwordHash;
        private Role role;
        private String institution;
        private boolean isActive;
        private LocalDateTime createdAt;

        public UserBuilder id(UUID id) { this.id = id; return this; }
        public UserBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserBuilder username(String username) { this.username = username; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public UserBuilder role(Role role) { this.role = role; return this; }
        public UserBuilder institution(String institution) { this.institution = institution; return this; }
        public UserBuilder isActive(boolean isActive) { this.isActive = isActive; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public User build() {
            User user = new User();
            user.id = this.id;
            user.firstName = this.firstName;
            user.lastName = this.lastName;
            user.username = this.username;
            user.email = this.email;
            user.passwordHash = this.passwordHash;
            user.role = this.role;
            user.institution = this.institution;
            user.isActive = this.isActive;
            user.createdAt = this.createdAt;
            return user;
        }
    }
    
    /**
     * Factory method para crear un nuevo estudiante con valores por defecto.
     * Agrupa la lógica de inicialización para nuevos registros públicos.
     *
     * @param firstName    El nombre del usuario.
     * @param lastName     El apellido del usuario.
     * @param username     El nombre de usuario elegido (handle).
     * @param email        El correo electrónico del usuario.
     * @param passwordHash La contraseña ya cifrada.
     * @param institution  La institución a la que pertenece.
     * @return Una instancia de {@link User} lista para ser persistida.
     */
    public static User createNewStudent(String firstName, String lastName, String username, String email, String passwordHash, String institution) {
        return User.builder()
                .id(UUID.randomUUID()) // Genera un ID seguro
                .firstName(firstName)
                .lastName(lastName)
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

