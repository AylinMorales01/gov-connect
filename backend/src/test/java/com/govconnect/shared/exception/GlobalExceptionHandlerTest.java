package com.govconnect.shared.exception;

import com.govconnect.shared.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletResponse;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link GlobalExceptionHandler}.
 * Verifica que cada tipo de excepción se mapee al HTTP status y estructura correctos.
 */
@DisplayName("GlobalExceptionHandler — mapeo de excepciones")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletResponse httpResponse;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        httpResponse = mock(HttpServletResponse.class);
        when(httpResponse.isCommitted()).thenReturn(false);
    }

    @Nested
    @DisplayName("401 Unauthorized")
    class Unauthorized {

        @Test
        @DisplayName("BadCredentialsException → 401")
        void badCredentialsShouldReturn401() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleBadCredentials(new BadCredentialsException("bad"), httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().success()).isFalse();
            assertThat(response.getBody().data()).isNull();
        }

        @Test
        @DisplayName("AuthenticationException → 401")
        void authenticationExceptionShouldReturn401() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleAuthentication(new AuthenticationException("expired") {}, httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().success()).isFalse();
        }
    }

    @Nested
    @DisplayName("403 Forbidden")
    class Forbidden {

        @Test
        @DisplayName("AccessDeniedException → 403")
        void accessDeniedShouldReturn403() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleAccessDenied(new AccessDeniedException("no access"), httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().success()).isFalse();
        }
    }

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("IllegalArgumentException → 400")
        void illegalArgumentShouldReturn400() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleIllegalArgument(new IllegalArgumentException("invalid param"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().success()).isFalse();
        }
    }

    @Nested
    @DisplayName("500 Internal Server Error")
    class ServerError {

        @Test
        @DisplayName("SQLException → 500 con mensaje genérico")
        void sqlExceptionShouldReturn500() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleSqlException(new SQLException("table not found", "42S02", 208));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().success()).isFalse();
            // El mensaje al cliente NUNCA debe exponer detalles SQL
            assertThat(response.getBody().message()).doesNotContain("table");
            assertThat(response.getBody().message()).doesNotContain("42S02");
        }

        @Test
        @DisplayName("Exception genérica → 500")
        void genericExceptionShouldReturn500() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleGeneral(new RuntimeException("unexpected internal error"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().success()).isFalse();
            // Mensaje genérico, no el detalle interno
            assertThat(response.getBody().message()).doesNotContain("unexpected");
        }
    }

    @Nested
    @DisplayName("Estructura ApiResponse")
    class ApiResponseStructure {

        @Test
        @DisplayName("Todas las respuestas de error deben tener data=null")
        void errorResponsesShouldHaveNullData() {
            ResponseEntity<ApiResponse<Void>> response =
                    handler.handleBadCredentials(new BadCredentialsException("x"), httpResponse);

            assertThat(response.getBody().data()).isNull();
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }
}
