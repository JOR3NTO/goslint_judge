package co.uceva.auth.model;

/**
 * Roles disponibles en el sistema. Determinan los permisos de cada usuario.
 */
public enum Role {
    /** Administrador del sistema con acceso total. */
    ADMIN,
    /** Estudiante o competidor regular de la plataforma. */
    STUDENT,
    /** Organizador encargado de gestionar maratones y problemas. */
    ORGANIZER
}
