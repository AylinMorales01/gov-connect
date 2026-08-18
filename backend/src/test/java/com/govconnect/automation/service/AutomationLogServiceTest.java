package com.govconnect.automation.service;

import com.govconnect.automation.dto.AutomationLogRequest;
import com.govconnect.automation.dto.AutomationLogResponse;
import com.govconnect.automation.repository.AutomationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AutomationLogService — registro de automatizaciones")
@ExtendWith(MockitoExtension.class)
class AutomationLogServiceTest {

    @Mock
    private AutomationLogRepository repository;

    @InjectMocks
    private AutomationLogService service;

    private AutomationLogRequest request;

    @BeforeEach
    void setUp() {
        request = new AutomationLogRequest(
                1L,                    // userId
                "n8n-import-daily",    // process
                "SUCCESS",             // status
                "Importación completada: 360 registros",  // message
                2500                   // executionTimeMs
        );
    }

    @Test
    @DisplayName("Debe delegar la inserción al repositorio con los mismos datos")
    void shouldDelegateInsertToRepository() {
        service.registerExecution(request);

        ArgumentCaptor<AutomationLogRequest> captor =
                ArgumentCaptor.forClass(AutomationLogRequest.class);
        verify(repository).insert(captor.capture());

        AutomationLogRequest captured = captor.getValue();
        assertThat(captured.process()).isEqualTo("n8n-import-daily");
        assertThat(captured.status()).isEqualTo("SUCCESS");
        assertThat(captured.executionTimeMs()).isEqualTo(2500);
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando no hay logs")
    void shouldReturnEmptyListWhenNoLogs() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<AutomationLogResponse> result = service.getAutomationLogs();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar los logs del repositorio")
    void shouldReturnLogsFromRepository() {
        AutomationLogResponse log = new AutomationLogResponse(
                1L, 1L, "test-process", "SUCCESS",
                "OK", 150, LocalDateTime.now()
        );
        when(repository.findAll()).thenReturn(List.of(log));

        List<AutomationLogResponse> result = service.getAutomationLogs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).process()).isEqualTo("test-process");
        assertThat(result.get(0).status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Debe manejar request con userId null (automatización sin usuario)")
    void shouldHandleNullUserId() {
        AutomationLogRequest noUser = new AutomationLogRequest(
                null, "scheduled-job", "SUCCESS", "OK", 100
        );

        service.registerExecution(noUser);

        ArgumentCaptor<AutomationLogRequest> captor =
                ArgumentCaptor.forClass(AutomationLogRequest.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().userId()).isNull();
    }

    @Test
    @DisplayName("Debe manejar status ERROR correctamente")
    void shouldHandleErrorStatus() {
        AutomationLogRequest errorRequest = new AutomationLogRequest(
                1L, "failing-job", "ERROR", "Connection timeout", 30000
        );

        service.registerExecution(errorRequest);

        ArgumentCaptor<AutomationLogRequest> captor =
                ArgumentCaptor.forClass(AutomationLogRequest.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo("ERROR");
    }
}
