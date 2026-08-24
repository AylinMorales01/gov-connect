package com.govconnect.automation.service;

import com.govconnect.automation.config.ExpiringContractsAlertProperties;
import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.ExpiringContractAlertItem;
import com.govconnect.automation.dto.ExpiringContractsAlertResponse;
import com.govconnect.automation.repository.ExpiringContractsRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link ExpiringContractsAlertService}.
 * Verifica los escenarios de degradación controlada (sin destinatarios, sin
 * SMTP, sin contratos, fallo de envío) y el envío correcto de la alerta.
 */
@DisplayName("ExpiringContractsAlertService — alerta de contratos por vencer")
@ExtendWith(MockitoExtension.class)
class ExpiringContractsAlertServiceTest {

    private static final String FROM = "no-reply@govconnect.com";
    private static final String RECIPIENT = "admin@govconnect.com";
    private static final String CRON = "0 0 7 * * MON-FRI";

    @Mock
    private ExpiringContractsRepository repository;

    @Mock
    private EmailService emailService;

    @Mock
    private AutomationLogService automationLogService;

    private ExpiringContractsAlertService service(
            boolean enabled, List<String> recipients) {
        return new ExpiringContractsAlertService(
                repository,
                emailService,
                automationLogService,
                new ExpiringContractsAlertProperties(enabled, recipients, 30, FROM, CRON)
        );
    }

    private ExpiringContractAlertItem item(String number, int days) {
        return new ExpiringContractAlertItem(
                number,
                "Contratista " + number,
                "Objeto de prueba",
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(days),
                days,
                "Infraestructura"
        );
    }

    @Test
    @DisplayName("envía un correo por destinatario cuando hay contratos y SMTP configurado")
    void sendsEmailWhenContractsAndSmtpConfigured() throws Exception {
        when(emailService.isConfigured()).thenReturn(true);
        when(repository.findExpiring(30)).thenReturn(List.of(item("C-1", 10), item("C-2", 20)));

        ExpiringContractsAlertResponse result = service(true, List.of(RECIPIENT)).runAlert();

        assertThat(result.contractsFound()).isEqualTo(2);
        assertThat(result.emailsSent()).isEqualTo(1);
        verify(emailService).sendHtml(eq(FROM), eq(RECIPIENT), anyString(), anyString());
        assertLogStatus("SUCCESS");
    }

    @Test
    @DisplayName("no envía y registra SKIPPED cuando no hay destinatarios")
    void skipsWhenNoRecipients() throws Exception {
        ExpiringContractsAlertResponse result = service(true, List.of()).runAlert();

        assertThat(result.emailsSent()).isZero();
        assertThat(result.contractsFound()).isZero();
        assertThat(result.message()).contains("destinatarios");
        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString(), anyString());
        verify(repository, never()).findExpiring(anyInt());
        assertLogStatus("SKIPPED");
    }

    @Test
    @DisplayName("no envía y registra ERROR cuando SMTP no está configurado")
    void skipsWhenSmtpNotConfigured() {
        when(emailService.isConfigured()).thenReturn(false);

        ExpiringContractsAlertResponse result = service(true, List.of(RECIPIENT)).runAlert();

        assertThat(result.emailsSent()).isZero();
        assertThat(result.message()).contains("SMTP");
        verify(repository, never()).findExpiring(anyInt());
        assertLogStatus("ERROR");
    }

    @Test
    @DisplayName("no envía cuando no hay contratos por vencer")
    void noEmailWhenNoContracts() throws Exception {
        when(emailService.isConfigured()).thenReturn(true);
        when(repository.findExpiring(30)).thenReturn(List.of());

        ExpiringContractsAlertResponse result = service(true, List.of(RECIPIENT)).runAlert();

        assertThat(result.contractsFound()).isZero();
        assertThat(result.emailsSent()).isZero();
        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString(), anyString());
        assertLogStatus("SUCCESS");
    }

    @Test
    @DisplayName("registra ERROR y devuelve 0 enviados cuando falla el envío")
    void returnsErrorWhenSendFails() throws Exception {
        when(emailService.isConfigured()).thenReturn(true);
        when(repository.findExpiring(30)).thenReturn(List.of(item("C-1", 10)));
        doThrow(new MessagingException("timeout"))
                .when(emailService).sendHtml(anyString(), anyString(), anyString(), anyString());

        ExpiringContractsAlertResponse result = service(true, List.of(RECIPIENT)).runAlert();

        assertThat(result.contractsFound()).isEqualTo(1);
        assertThat(result.emailsSent()).isZero();
        assertThat(result.message()).contains("Error");
        assertLogStatus("ERROR");
    }

    @Test
    @DisplayName("la alerta programada no hace nada cuando está deshabilitada")
    void scheduledAlertDoesNothingWhenDisabled() {
        service(false, List.of(RECIPIENT)).scheduledAlert();

        verify(repository, never()).findExpiring(anyInt());
        verify(automationLogService, never()).registerExecution(any());
    }

    private void assertLogStatus(String status) {
        ArgumentCaptor<AutomationLogRequest> captor =
                ArgumentCaptor.forClass(AutomationLogRequest.class);
        verify(automationLogService).registerExecution(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(status);
        assertThat(captor.getValue().process()).isEqualTo("expiring-contracts-alert");
    }
}
