package com.govconnect.automation.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link EmailService}.
 * Verifica la detección de SMTP no configurado y el envío de un mensaje HTML.
 */
@DisplayName("EmailService — envío de correo")
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;

    @Mock
    private JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailService(provider);
    }

    @Test
    @DisplayName("isConfigured devuelve false cuando no hay JavaMailSender")
    void isConfiguredReturnsFalseWithoutSender() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(service.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("isConfigured devuelve true cuando existe JavaMailSender")
    void isConfiguredReturnsTrueWithSender() {
        when(provider.getIfAvailable()).thenReturn(mailSender);

        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("sendHtml lanza IllegalStateException sin SMTP configurado")
    void sendHtmlThrowsWithoutSender() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.sendHtml("from@x.com", "to@x.com", "s", "<b>h</b>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sendHtml construye el MimeMessage y lo envía")
    void sendHtmlSends() throws Exception {
        when(provider.getIfAvailable()).thenReturn(mailSender);
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendHtml("from@x.com", "to@x.com", "Asunto", "<b>Hola</b>");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Asunto");
        assertThat(message.getAllRecipients()).hasSize(1);
    }
}
