package com.govconnect.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govconnect.auth.security.JwtAuthenticationFilter;
import com.govconnect.auth.service.CustomUserDetailsService;
import com.govconnect.shared.constants.ApiMessages;
import com.govconnect.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    /** Si Swagger está habilitado (desarrollo), sus rutas son públicas. */
    @Value("${swagger.enabled:true}")
    private boolean swaggerEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF deshabilitado: la autenticación usa cookies HttpOnly con
                // SameSite=Lax, que impide el envío en peticiones cross-site.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    // Sesión actual: requiere autenticación (lee la cookie HttpOnly).
                    // Debe ir antes del permitAll de /api/v1/auth/** para que aplique.
                    auth.requestMatchers("/api/v1/auth/me").authenticated();

                    // Endpoints públicos (sin autenticación)
                    auth.requestMatchers(
                            "/api/v1/auth/**",
                            "/error"
                    ).permitAll();

                    // Swagger solo público cuando está habilitado (desarrollo)
                    if (swaggerEnabled) {
                        auth.requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll();
                    }

                    // Endpoints exclusivos de ADMIN
                    auth.requestMatchers(
                            "/api/v1/dashboard/**"
                    ).hasRole("ADMIN");

                    // Endpoints para usuarios autenticados (USER o ADMIN)
                    auth.requestMatchers(
                            "/api/v1/analytics/**",
                            "/api/v1/automation/**"
                    ).authenticated();

                    // Cualquier otra petición requiere autenticación
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 401 Unauthorized — solo escribe si el response no fue commiteado aún
                            if (response.isCommitted()) {
                                return;
                            }
                            try {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                objectMapper.writeValue(
                                        response.getOutputStream(),
                                        ApiResponse.error(ApiMessages.AUTH_UNAUTHORIZED)
                                );
                            } catch (java.io.IOException e) {
                                // El response ya estaba siendo escrito — no hay acción posible
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 403 Forbidden — solo escribe si el response no fue commiteado aún
                            if (response.isCommitted()) {
                                return;
                            }
                            try {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                objectMapper.writeValue(
                                        response.getOutputStream(),
                                        ApiResponse.error(ApiMessages.AUTH_FORBIDDEN)
                                );
                            } catch (java.io.IOException e) {
                                // El response ya estaba siendo escrito — no hay acción posible
                            }
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Encabezados exactos que necesita el frontend — sin wildcard
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
