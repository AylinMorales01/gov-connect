package com.govconnect.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa un usuario del sistema.
 * Implementa {@link UserDetails} para integrarse con Spring Security.
 * <p>
 * Soporta roles {@code ADMIN} y {@code USER} para autorización
 * basada en rutas ({@code /dashboard/**} exclusivo de ADMIN).
 * </p>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Versión del token para invalidación server-side de refresh tokens.
     * <p>
     * Se incrementa en cada logout o cambio de contraseña, invalidando
     * todos los refresh tokens emitidos anteriormente para este usuario.
     * </p>
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── UserDetails implementation ──────────────────────

    @Override
    public String getPassword() {
        return passwordHash;
    }

    // Cache lazy de autoridades para no reinstanciar en cada llamada
    @jakarta.persistence.Transient
    private transient Collection<? extends GrantedAuthority> authoritiesCache;

    /**
     * Devuelve la autoridad basada en el rol del usuario.
     * <p>
     * Spring Security espera el prefijo "ROLE_".
     * Rol ADMIN  → ROLE_ADMIN
     * Rol USER   → ROLE_USER
     * </p>
     * <p>
     * El resultado se calcula una sola vez y se cachea en memoria
     * durante la vida de la entidad.
     * </p>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authoritiesCache == null) {
            authoritiesCache = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authoritiesCache;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
