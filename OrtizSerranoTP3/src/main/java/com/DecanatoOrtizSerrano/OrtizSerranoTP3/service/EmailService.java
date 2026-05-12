package com.DecanatoOrtizSerrano.OrtizSerranoTP3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de correo electrónico.
 *
 * <p>Envía emails de bienvenida de forma asíncrona al crear usuarios desde el panel
 * de administración. Si el servidor SMTP no está configurado (modo desarrollo / H2),
 * el envío falla silenciosamente y se registra en el log.</p>
 *
 * <p>Para habilitar el envío real, configurar las propiedades {@code spring.mail.*}
 * en {@code application.properties}.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@decanato.edu.ar}")
    private String fromAddress;

    @Value("${app.nombre:Sistema de Decanato}")
    private String appNombre;

    @Value("${app.url:http://localhost:5173}")
    private String appUrl;

    /**
     * Envía el correo de bienvenida con las credenciales del nuevo usuario.
     *
     * <p>Se ejecuta de forma asíncrona para no bloquear la respuesta HTTP del
     * endpoint de creación. Si el envío falla, se registra el error pero la
     * creación del usuario ya fue confirmada.</p>
     *
     * @param destinatario Email del nuevo usuario
     * @param nombre       Nombre del usuario
     * @param apellido     Apellido del usuario
     * @param rol          Rol asignado (ESTUDIANTE / DOCENTE / ADMINISTRADOR)
     * @param passwordPlana Contraseña en texto plano (antes de hashear) para incluir en el email
     */
    @Async
    public void enviarBienvenida(String destinatario, String nombre, String apellido,
                                  String rol, String passwordPlana) {
        if (mailSender == null) {
            log.warn("[EmailService] JavaMailSender no configurado — email de bienvenida NO enviado a {}. " +
                     "Configurar spring.mail.* para habilitar el envío real.", destinatario);
            // ⚠️  NO loguear la contraseña en texto plano: es un riesgo de seguridad.
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(destinatario);
            helper.setSubject("Bienvenido/a al " + appNombre + " — Tus credenciales de acceso");
            helper.setText(construirHtml(nombre, apellido, rol, destinatario, passwordPlana), true);

            mailSender.send(mensaje);
            log.info("[EmailService] Bienvenida enviada a {}", destinatario);

        } catch (MessagingException | MailException ex) {
            log.error("[EmailService] Error al enviar bienvenida a {}: {}", destinatario, ex.getMessage());
        }
    }

    // ── HTML del correo ───────────────────────────────────────────────────────

    private String construirHtml(String nombre, String apellido, String rol,
                                  String email, String password) {
        String rolLabel = switch (rol.toUpperCase()) {
            case "ESTUDIANTE"    -> "Estudiante";
            case "DOCENTE"       -> "Docente";
            case "ADMINISTRADOR" -> "Administrador";
            default              -> rol;
        };

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <style>
                body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                .card { background: #ffffff; border-radius: 8px; max-width: 560px;
                        margin: 0 auto; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,.1); }
                h1   { color: #1a237e; font-size: 22px; margin-bottom: 4px; }
                .sub { color: #555; font-size: 14px; margin-bottom: 24px; }
                .cred { background: #e8eaf6; border-radius: 6px; padding: 16px 20px; margin: 20px 0; }
                .cred p { margin: 6px 0; font-size: 15px; }
                .cred strong { color: #1a237e; }
                .btn { display: inline-block; background: #1a237e; color: #fff !important;
                       padding: 12px 28px; border-radius: 6px; text-decoration: none;
                       font-weight: bold; margin-top: 24px; font-size: 15px; }
                .footer { color: #999; font-size: 12px; margin-top: 28px; border-top: 1px solid #eee; padding-top: 16px; }
                .warn  { color: #b71c1c; font-size: 13px; margin-top: 12px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>¡Bienvenido/a, %s %s!</h1>
                <p class="sub">Tu cuenta en el <strong>%s</strong> ha sido creada con el rol <strong>%s</strong>.</p>

                <p>A continuación encontrás tus credenciales de acceso:</p>
                <div class="cred">
                  <p>📧 <strong>Usuario (email):</strong> %s</p>
                  <p>🔑 <strong>Contraseña temporal:</strong> %s</p>
                </div>

                <p class="warn">⚠️ Por seguridad, cambiá tu contraseña la primera vez que ingreses al sistema.</p>

                <a href="%s/login" class="btn">Ingresar al sistema</a>

                <div class="footer">
                  Este correo fue generado automáticamente. No respondas a este mensaje.<br/>
                  Si no esperabas este correo, ignoralo o contactá al área de sistemas.
                </div>
              </div>
            </body>
            </html>
            """.formatted(nombre, apellido, appNombre, rolLabel, email, password, appUrl);
    }
}
