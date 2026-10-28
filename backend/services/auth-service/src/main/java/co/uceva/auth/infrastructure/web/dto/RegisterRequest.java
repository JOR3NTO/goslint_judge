package co.uceva.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * Data Transfer Object (DTO) que mapea la petición JSON del cliente para registro.
 * Contiene validaciones integradas de Jakarta Validation para rechazar
 * peticiones mal formadas antes de que lleguen a la capa de negocio.
 *
 * Campos esperados del frontend (register-page.tsx):
 * - firstName: "Nombre" (input firstName)
 * - lastName: "Apellido" (input lastName)
 * - username: "Nombre de Usuario" (input username)
 * - email: "Correo Electrónico" (input email)
 * - password: "Contraseña" (input password)
 * - institution: (opcional, no visible en el form actual)
 */
public class RegisterRequest {

    /** Nombre del usuario. Obligatorio, entre 2 y 50 caracteres. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String firstName;

    /** Apellido del usuario. Obligatorio, entre 2 y 50 caracteres. */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String lastName;

    /** Nombre de usuario (handle). Es obligatorio y debe tener entre 3 y 50 caracteres. */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    /** Correo electrónico. Es obligatorio y debe tener un formato válido (ej. x@y.com). */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Debe proporcionar un correo electrónico válido")
    private String email;

    /**
     * Contraseña proporcionada por el usuario (en texto plano en la petición).
     * Debe cumplir los mismos requisitos que muestra el frontend:
     * - Mínimo 8 caracteres
     * - Al menos una letra mayúscula
     * - Al menos un número
     * - Al menos un carácter especial
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "La contraseña debe contener al menos una mayúscula, un número y un carácter especial"
    )
    private String password;

    /** Nombre de la institución a la que pertenece (opcional). */
    private String institution;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
}

