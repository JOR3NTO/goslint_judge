package co.uceva.contest.domain.service;

import java.util.UUID;

/** Puerto de salida para notificar por correo las invitaciones a un equipo. */
public interface EmailSender {

    void sendInvitation(String to, String teamName, String contestTitle, UUID invitationId);
}
