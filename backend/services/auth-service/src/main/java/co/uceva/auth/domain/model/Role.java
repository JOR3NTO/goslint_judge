package co.uceva.auth.domain.model;

/**
 * Enumeración que define los roles disponibles en el sistema.
 * Determina los niveles de acceso y permisos de cada usuario.
 */
public enum Role {
    /** Administrador del sistema con acceso total. */
    ADMIN,
    /** Estudiante o competidor regular de la plataforma. */
    STUDENT,
    /** Organizador encargado de gestionar maratones y problemas. */
    ORGANIZER
}
