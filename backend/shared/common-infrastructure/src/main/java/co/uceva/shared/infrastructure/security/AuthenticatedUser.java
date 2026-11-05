package co.uceva.shared.infrastructure.security;

import java.util.UUID;

/**
 * Identidad extraída de un token ya validado.
 * <p>
 * Representa a quién pertenece una petición o una conexión, sin arrastrar el
 * token en sí: una vez comprobada la firma, el resto del sistema trabaja con
 * este objeto y nunca vuelve a interpretar la cadena original.
 * </p>
 *
 * @param userId Identificador del usuario, tomado del claim {@code sub}.
 * @param role   Rol del usuario, tomado del claim {@code role}.
 */
public record AuthenticatedUser(UUID userId, String role) {}
