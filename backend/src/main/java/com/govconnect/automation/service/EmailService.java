package com.govconnect.automation.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correo electrónico (Spring Mail / SMTP).
 * <p>
 * Envuelve el {@link JavaMailSender} auto-configurado por Spring Boot. Cuando
 * {@code spring.mail.host} está vacío, Spring Boot no crea el bean, por lo que
 * se inyecta mediante {@link ObjectProvider} y se consulta su disponibilidad
 * con {@link #isConfigured()}. Así la aplicación arranca sin SMTP y la alerta
 * puede registrar el fallo en {@code automation_logs} en vez de detenerse.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * Indica si el envío de correo está disponible (SMTP configurado).
     *
     * @return {@code true} si existe un {@link JavaMailSender} en el contexto.
     */
    public boolean isConfigured() {
        return mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * Envía un correo HTML a un único destinatario.
     *
     * @param from    remitente.
     * @param to      destinatario.
     * @param subject asunto.
     * @param html    cuerpo del mensaje en HTML.
     * @throws MessagingException     si falla la construcción o el envío del mensaje.
     * @throws IllegalStateException  si SMTP no está configurado.
     */
    public void sendHtml(String from, String to, String subject, String html) throws MessagingException {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP no configurado (spring.mail.host vacío)");
        }

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        sender.send(message);
        log.info("Correo enviado a {} con asunto '{}'", to, subject);
    }
}
