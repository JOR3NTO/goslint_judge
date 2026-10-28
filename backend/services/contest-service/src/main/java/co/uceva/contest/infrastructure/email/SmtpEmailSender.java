package co.uceva.contest.infrastructure.email;

import java.util.UUID;

import co.uceva.contest.domain.service.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Implementación SMTP del puerto {@link EmailSender}. Envía la invitación en HTML con la
 * identidad visual del frontend (fondo oscuro y verde neón) y una versión en texto plano.
 * Un fallo de envío propaga {@link org.springframework.mail.MailException}.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final InvitationLinks links;
    private final String from;
    private final String frontendUrl;

    public SmtpEmailSender(JavaMailSender mailSender, InvitationLinks links,
                           @Value("${app.mail.from}") String from,
                           @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.links = links;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    /** {@inheritDoc} */
    @Override
    public void sendInvitation(String to, String teamName, String contestTitle, UUID invitationId) {
        String acceptUrl = links.respondUrl(invitationId, "accept");
        String rejectUrl = links.respondUrl(invitationId, "reject");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Invitación al equipo " + teamName + " · Goslint Judge");
            helper.setText(plainText(teamName, contestTitle, acceptUrl, rejectUrl),
                    html(HtmlUtils.htmlEscape(teamName), HtmlUtils.htmlEscape(contestTitle), acceptUrl, rejectUrl));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("No se pudo construir el correo de invitación.", e);
        }
    }

    private String plainText(String teamName, String contestTitle, String acceptUrl, String rejectUrl) {
        return """
                Te invitaron a unirte al equipo "%s" para la maratón "%s" en Goslint Judge.

                Aceptar: %s
                Rechazar: %s

                Los equipos tienen máximo 3 integrantes: si el equipo se llena antes de que aceptes, \
                la invitación deja de ser válida. Solo puedes responder antes de que inicie la maratón.
                """.formatted(teamName, contestTitle, acceptUrl, rejectUrl);
    }

    // Tablas y estilos en línea: es lo único que respetan Gmail y Outlook.
    private String html(String teamName, String contestTitle, String acceptUrl, String rejectUrl) {
        return """
                <!doctype html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="color-scheme" content="dark"><title>Invitación a equipo</title></head>
                <body style="margin:0;padding:0;background:#020405;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#020405" style="background:#020405;">
                <tr><td align="center" style="padding:32px 16px;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;font-family:Geist,'Segoe UI',Helvetica,Arial,sans-serif;">
                    <tr><td style="padding:0 4px 20px 4px;">
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td width="36" height="36" align="center" valign="middle" style="width:36px;height:36px;border:1px solid #1f5a28;border-radius:8px;background:#0b1a0e;color:#45cd55;font-family:'Geist Mono',Consolas,monospace;font-size:14px;font-weight:700;">&lt;/&gt;</td>
                        <td style="padding-left:10px;font-size:20px;font-weight:700;color:#eeeeee;">Goslint<span style="color:#45cd55;">Judge</span></td>
                      </tr></table>
                    </td></tr>
                    <tr><td bgcolor="#05080b" style="background:#05080b;border:1px solid #19232a;border-radius:12px;padding:32px 28px;">
                      <p style="margin:0 0 8px 0;font-family:'Geist Mono',Consolas,monospace;font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#45cd55;">Invitación a equipo</p>
                      <h1 style="margin:0 0 16px 0;font-size:24px;line-height:1.3;color:#eeeeee;">Te invitaron a unirte a <span style="color:#45cd55;">%s</span></h1>
                      <p style="margin:0 0 24px 0;font-size:15px;line-height:1.6;color:#a8a8a8;">El capitán del equipo quiere que participes con ellos en la maratón <strong style="color:#eeeeee;">%s</strong>.</p>
                      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;"><tr>
                        <td bgcolor="#45cd55" style="border-radius:10px;background:#45cd55;">
                          <a href="%s" style="display:inline-block;padding:13px 28px;font-size:15px;font-weight:700;color:#020405;text-decoration:none;border-radius:10px;">Aceptar invitación</a>
                        </td>
                        <td width="12" style="width:12px;"></td>
                        <td style="border:1px solid #19232a;border-radius:10px;background:#0e1216;">
                          <a href="%s" style="display:inline-block;padding:12px 24px;font-size:15px;font-weight:600;color:#eeeeee;text-decoration:none;border-radius:10px;">Rechazar</a>
                        </td>
                      </tr></table>
                      <p style="margin:0;padding:14px 16px;border-left:3px solid #45cd55;background:#0e1216;border-radius:6px;font-size:13px;line-height:1.6;color:#a8a8a8;">Los equipos tienen <strong style="color:#eeeeee;">máximo 3 integrantes</strong>. Si el equipo se llena antes de que aceptes, la invitación deja de ser válida. Solo puedes responder antes de que inicie la maratón.</p>
                    </td></tr>
                    <tr><td style="padding:20px 4px 0 4px;font-size:12px;line-height:1.6;color:#808080;">
                      Si no esperabas este correo, puedes ignorarlo.<br>
                      <a href="%s" style="color:#45cd55;text-decoration:none;">Goslint Judge</a> · Semillero Goslint, UCEVA
                    </td></tr>
                  </table>
                </td></tr></table>
                </body></html>
                """.formatted(teamName, contestTitle, acceptUrl, rejectUrl, frontendUrl);
    }
}
