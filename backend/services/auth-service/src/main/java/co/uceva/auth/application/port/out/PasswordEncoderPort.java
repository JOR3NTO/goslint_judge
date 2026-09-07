package co.uceva.auth.application.port.out;

/**
 * Puerto de Salida (Output Port) para el cifrado de contraseñas.
 * Define el contrato que debe cumplir cualquier librería de encriptación (ej. BCrypt).
 */
public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
