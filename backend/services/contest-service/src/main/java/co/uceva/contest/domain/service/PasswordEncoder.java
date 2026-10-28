package co.uceva.contest.domain.service;

/**
 * Servicio de Dominio para el cifrado y validación de contraseñas de concursos.
 * Define el contrato que debe cumplir la tecnología de encriptación (ej. BCrypt)
 * sin acoplar el dominio a librerías externas o frameworks.
 */
public interface PasswordEncoder {

    /**
     * Cifra una contraseña en texto plano.
     *
     * @param rawPassword Contraseña en texto plano.
     * @return Hash cifrado de la contraseña.
     */
    String encode(String rawPassword);

    /**
     * Comprueba si una contraseña en texto plano coincide con su versión cifrada.
     *
     * @param rawPassword     Contraseña en texto plano.
     * @param encodedPassword Contraseña cifrada (hash).
     * @return {@code true} si coinciden, {@code false} en caso contrario.
     */
    boolean matches(String rawPassword, String encodedPassword);
}

