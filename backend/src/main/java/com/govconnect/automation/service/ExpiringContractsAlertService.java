package com.govconnect.automation.service;

import com.govconnect.automation.config.ExpiringContractsAlertProperties;
import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.ExpiringContractAlertItem;
import com.govconnect.automation.dto.ExpiringContractsAlertResponse;
import com.govconnect.automation.repository.ExpiringContractsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de la alerta de contratos por vencer (G3).
 * <p>
 * Detecta los contratos activos que vencen dentro de la ventana configurada
 * ({@code app.alert.expiring-contracts.days}), compone un correo HTML con la
 * tabla de contratos y lo envía a los destinatarios configurados mediante
 * {@link EmailService}. Cada ejecución queda auditada en {@code automation_logs}
 * (proceso {@code expiring-contracts-alert}).
 * </p>
 * <p>
 * La ejecución puede ser programada (cron) o manual (endpoint ADMIN). En ambos
 * casos se degrada de forma controlada: sin destinatarios, sin SMTP o sin
 * contratos por vencer, se registra el motivo y se devuelve un resumen en vez
 * de lanzar una excepción.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiringContractsAlertService {

    private static final String PROCESS = "expiring-contracts-alert";

    private final ExpiringContractsRepository repository;
    private final EmailService emailService;
    private final AutomationLogService automationLogService;
    private final ExpiringContractsAlertProperties properties;

    /**
     * Ejecución programada de la alerta (cron de Spring).
     * <p>
     * Respeta {@code app.alert.expiring-contracts.enabled}: si está deshabilitada,
     * no hace nada (el disparo manual sigue disponible vía {@link #runAlert()}).
     * </p>
     */
    @Scheduled(cron = "${app.alert.expiring-contracts.cron:0 0 7 * * MON-FRI}")
    public void scheduledAlert() {
        if (!properties.enabled()) {
            log.info("Alerta automática de contratos por vencer deshabilitada "
                    + "(app.alert.expiring-contracts.enabled=false)");
            return;
        }
        runAlert();
    }

    /**
     * Ejecuta la alerta de forma manual o programada.
     * <p>
     * No lanza excepciones para los escenarios esperados (sin destinatarios,
     * SMTP ausente o sin contratos); en todos los casos devuelve un resumen con
     * el motivo y deja el resultado registrado en {@code automation_logs}.
     * </p>
     *
     * @return resumen con contratos encontrados, correos enviados y mensaje de resultado.
     */
    public ExpiringContractsAlertResponse runAlert() {
        long start = System.currentTimeMillis();

        List<String> recipients = normalizedRecipients();
        if (recipients.isEmpty()) {
            String message = "No hay destinatarios configurados (app.alert.expiring-contracts.recipients)";
            log.warn("{}: {}", PROCESS, message);
            recordLog("SKIPPED", message, start);
            return new ExpiringContractsAlertResponse(0, 0, List.of(), message);
        }

        if (!emailService.isConfigured()) {
            String message = "SMTP no configurado (spring.mail.host vacío). La alerta no se envió.";
            log.warn("{}: {}", PROCESS, message);
            recordLog("ERROR", message, start);
            return new ExpiringContractsAlertResponse(0, 0, recipients, message);
        }

        List<ExpiringContractAlertItem> contracts = repository.findExpiring(properties.days());

        if (contracts.isEmpty()) {
            String message = "No hay contratos por vencer en los próximos " + properties.days() + " días.";
            log.info("{}: {}", PROCESS, message);
            recordLog("SUCCESS", message, start);
            return new ExpiringContractsAlertResponse(0, 0, recipients, message);
        }

        try {
            String subject = subjectFor(contracts.size());
            String html = buildHtml(contracts);
            for (String recipient : recipients) {
                emailService.sendHtml(properties.from(), recipient, subject, html);
            }

            String message = "Alerta enviada a " + recipients.size() + " destinatario(s) con "
                    + contracts.size() + " contrato(s) por vencer.";
            log.info("{}: {}", PROCESS, message);
            recordLog("SUCCESS", message, start);
            return new ExpiringContractsAlertResponse(contracts.size(), recipients.size(), recipients, message);
        } catch (Exception e) {
            String message = "Error al enviar la alerta: " + e.getMessage();
            log.error("{}: {}", PROCESS, message, e);
            recordLog("ERROR", message, start);
            return new ExpiringContractsAlertResponse(contracts.size(), 0, recipients, message);
        }
    }

    /**
     * Filtra los destinatarios configurados para descartar valores nulos o en blanco.
     */
    private List<String> normalizedRecipients() {
        if (properties.recipients() == null) {
            return List.of();
        }
        return properties.recipients().stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .toList();
    }

    private String subjectFor(int count) {
        return count == 1
                ? "1 contrato por vencer — Gov Connect"
                : count + " contratos por vencer — Gov Connect";
    }

    /**
     * Compone el cuerpo HTML del correo con una tabla de contratos por vencer.
     */
    private String buildHtml(List<ExpiringContractAlertItem> contracts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style=\"font-family:Arial,Helvetica,sans-serif;color:#1f2937;font-size:14px;\">");
        sb.append("<h2>Contratos por vencer — Gov Connect</h2>");
        sb.append("<p>Los siguientes <strong>").append(contracts.size()).append("</strong> contrato(s) ")
                .append("vence(n) dentro de los próximos <strong>").append(properties.days()).append("</strong> días:</p>");
        sb.append("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;\">");
        sb.append("<thead><tr style=\"background:#f3f4f6;\">")
                .append("<th>Contrato</th><th>Contratista</th><th>Objeto</th><th>Valor</th>")
                .append("<th>Vence</th><th>Días</th><th>Dependencia</th>")
                .append("</tr></thead><tbody>");
        for (ExpiringContractAlertItem c : contracts) {
            sb.append("<tr>")
                    .append("<td>").append(esc(c.contractNumber())).append("</td>")
                    .append("<td>").append(esc(c.contractorName())).append("</td>")
                    .append("<td>").append(esc(c.object())).append("</td>")
                    .append("<td>$ ").append(c.contractValue() == null ? "" : c.contractValue().toPlainString()).append("</td>")
                    .append("<td>").append(c.endDate()).append("</td>")
                    .append("<td>").append(c.remainingDays()).append("</td>")
                    .append("<td>").append(esc(c.department())).append("</td>")
                    .append("</tr>");
        }
        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }

    /**
     * Escapa caracteres especiales de HTML para evitar inyección de markup
     * desde valores provenientes de la base de datos.
     */
    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Registra el resultado en {@code automation_logs} sin que un fallo de
     * auditoría interrumpa el flujo principal de la alerta.
     */
    private void recordLog(String status, String message, long startMillis) {
        try {
            automationLogService.registerExecution(new AutomationLogRequest(
                    null,
                    PROCESS,
                    status,
                    message,
                    (int) (System.currentTimeMillis() - startMillis)
            ));
        } catch (Exception ex) {
            log.warn("No se pudo registrar la ejecución en automation_logs: {}", ex.getMessage());
        }
    }
}
