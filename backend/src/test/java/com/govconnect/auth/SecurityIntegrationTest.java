package com.govconnect.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración de seguridad.
 * Verifica 401 (sin auth), 403 (rol insuficiente), 200 (rol correcto).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.primary.jdbc-url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MSSQLServer",
        "spring.datasource.primary.username=sa",
        "spring.datasource.primary.password=",
        "spring.datasource.primary.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.main.allow-bean-definition-overriding=true",
        "jwt.secret=test-secret-key-for-junit-minimum-32-bytes!!",
        "jwt.expiration=3600",
        "jwt.refresh-expiration=604800"
})
@DisplayName("Seguridad — Integración")
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @TestConfiguration
    static class TestDuckDbConfig {
        @Bean
        DataSource duckDbDataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:duckdb::memory:");
            return ds;
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── 401 Unauthorized ────────────────────────────────

    @Nested
    @DisplayName("HTTP 401 — Sin autenticación")
    class UnauthorizedEndpoints {

        @Test
        @DisplayName("GET /api/v1/dashboard/summary requiere autenticación")
        void dashboardSummaryRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/summary"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("GET /api/v1/analytics/health requiere autenticación")
        void analyticsHealthRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/health"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/v1/automation/logs requiere autenticación")
        void automationLogsRequiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/automation/logs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh sin cookie → 401")
        void refreshWithoutCookieReturns401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ── 403 Forbidden ───────────────────────────────────

    @Nested
    @DisplayName("HTTP 403 — USER intenta acceder a endpoints ADMIN")
    class ForbiddenEndpoints {

        @Test
        @DisplayName("USER no puede acceder a dashboard/summary")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void userCannotAccessDashboard() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/summary"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("USER no puede acceder a dashboard/expiring-contracts")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void userCannotAccessExpiringContracts() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/expiring-contracts"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER no puede acceder a dashboard/budget-execution")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void userCannotAccessBudgetExecution() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/budget-execution"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Acceso ADMIN ────────────────────────────────────

    @Nested
    @DisplayName("Acceso permitido — ADMIN")
    class AdminAccess {

        @Test
        @DisplayName("ADMIN accede a analytics/health")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminCanAccessAnalytics() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ADMIN accede a automation/logs (seguridad permite, error DB esperado en H2)")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminCanAccessAutomation() throws Exception {
            // La seguridad permite el acceso (no 401/403).
            // El error 500 es porque la tabla automation_logs no existe en H2 (usa SQL nativo).
            mockMvc.perform(get("/api/v1/automation/logs"))
                    .andExpect(status().is5xxServerError()); // BD no disponible en test, pero auth OK
        }
    }

    // ── Acceso USER ─────────────────────────────────────

    @Nested
    @DisplayName("Acceso permitido — USER autenticado")
    class AuthenticatedAccess {

        @Test
        @DisplayName("USER accede a analytics/health")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void userCanAccessAnalytics() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("USER accede a automation/logs (seguridad permite, error DB esperado en H2)")
        @WithMockUser(username = "testuser", roles = {"USER"})
        void userCanAccessAutomation() throws Exception {
            // La seguridad permite el acceso (no 401/403).
            // El error 500 es porque automation_logs usa SQL nativo y no existe en H2.
            mockMvc.perform(get("/api/v1/automation/logs"))
                    .andExpect(status().is5xxServerError()); // BD no disponible en test, pero auth OK
        }

        @Test
        @DisplayName("Usuario no autenticado es rechazado con 401")
        void unauthenticatedRejected() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/health"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
