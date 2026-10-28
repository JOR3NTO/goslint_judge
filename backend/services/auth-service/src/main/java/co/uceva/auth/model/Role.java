package co.uceva.auth.model;

/**
 * Roles disponibles en el sistema. Determinan los permisos de cada usuario.
 */
public enum Role {
    /**
     * Dueño de la plataforma. Es el único que asigna y retira administradores.
     * No se obtiene por registro ni por la API: se crea al arrancar el servicio
     * (ver {@code SuperAdminInitializer}).
     */
    SUPER_ADMIN,
    /** Administrador: gestiona estudiantes y organizadores, pero no a otros administradores. */
    ADMIN,
    /** Estudiante o competidor regular de la plataforma. */
    STUDENT,
    /** Organizador encargado de gestionar maratones y problemas. */
    ORGANIZER
}
