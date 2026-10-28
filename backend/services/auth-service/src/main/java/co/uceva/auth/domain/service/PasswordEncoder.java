package co.uceva.auth.domain.service;

/**
 * Servicio de Dominio para el cifrado de contraseñas.
 * Define el contrato que debe cumplir cualquier librería de encriptación (ej. BCrypt).
 */
public interface PasswordEncoder {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
