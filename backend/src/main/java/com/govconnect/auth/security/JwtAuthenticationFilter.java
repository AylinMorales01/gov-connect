package com.govconnect.auth.security;

import com.govconnect.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filtro que intercepta cada petición HTTP para extraer y validar
 * un JWT. Extrae el token de la cookie HttpOnly {@code access_token}
 * o, como retrocompatibilidad, del header {@code Authorization: Bearer <token>}.
 * <p>
 * <b>Comportamiento:</b>
 * <ul>
 *   <li>Si no hay token continúa sin autenticar (sin error).</li>
 *   <li>Si el token es válido establece el {@code SecurityContext}.</li>
 *   <li>Si el token es inválido/expirado limpia el contexto y continúa.</li>
 * </ul>
 * <p>
 * </b> extrae el rol del claim {@code role} del JWT
 * y lo convierte en {@code ROLE_<rol>} para Spring Security.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_COOKIE = "access_token";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        // Sin token: continuar sin autenticar
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            // Solo autenticar si no hay autenticación previa en el contexto
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Extraer rol del JWT y construir authorities
                String role = jwtService.extractRole(token);
                List<SimpleGrantedAuthority> authorities = (role != null)
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : Collections.emptyList();

                // Verificar que el usuario existe y está activo en BD
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    authorities
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado vía JWT: '{}' con rol '{}'", username, role);
                }
            }
        } catch (Exception e) {
            // Token inválido, expirado o usuario no encontrado.
            // No lanzamos excepción: limpiamos el contexto y dejamos
            // que Spring Security decida si el endpoint requiere auth.
            log.warn("JWT no válido o expirado: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT de la cookie {@code access_token} o, como
     * retrocompatibilidad, del header {@code Authorization: Bearer ...}.
     *
     * @return el token, o {@code null} si no está presente.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
