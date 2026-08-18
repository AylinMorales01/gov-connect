package com.govconnect.automation.service;

import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.AutomationLogResponse;
import com.govconnect.automation.repository.AutomationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de registrar y consultar las ejecuciones de
 * workflows de automatización provenientes de herramientas
 * externas como n8n.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationLogService {

    private final AutomationLogRepository repository;

    /**
     * Almacena un registro de ejecución de automatización.
     *
     * @param request DTO con los datos de la ejecución (nunca {@code null})
     */
    @Transactional
    public void registerExecution(AutomationLogRequest request) {
        log.info("Registrando ejecución de automatización: process={}, status={}",
                request.process(), request.status());
        repository.insert(request);
    }

    /**
     * Recupera el historial completo de ejecuciones de
     * automatización, ordenadas desde la más reciente.
     *
     * @return lista de registros (nunca {@code null}, puede estar vacía)
     */
    @Transactional(readOnly = true)
    public List<AutomationLogResponse> getAutomationLogs() {
        log.info("Consultando historial de automatizaciones");
        return repository.findAll();
    }
}
