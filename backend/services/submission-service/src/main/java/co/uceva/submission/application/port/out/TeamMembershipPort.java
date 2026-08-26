package co.uceva.submission.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida para averiguar qué usuarios componen un equipo.
 * <p>
 * Un envío se registra a nombre de un equipo, pero la notificación de su
 * veredicto se entrega a personas concretas. Este puerto es el único punto donde
 * se traduce lo uno en lo otro, y aísla al servicio de dónde vive esa
 * información: hoy la fuente será {@code contest-service}, que aún no existe.
 * </p>
 */
public interface TeamMembershipPort {

    /**
     * Recupera la composición de un equipo.
     * <p>
     * El contrato es tolerante a propósito: un equipo desconocido o una consulta
     * que falle se traducen en un equipo sin integrantes, no en una excepción. La
     * notificación es un extra sobre un veredicto que ya está persistido, y no
     * poder averiguar a quién avisar nunca debe hacer fracasar el registro del
     * resultado.
     * </p>
     *
     * @param teamId Identificador del equipo a consultar.
     * @return El equipo con sus integrantes; con la lista vacía si no se pudo determinar.
     */
    TeamDTO findTeam(UUID teamId);
}
