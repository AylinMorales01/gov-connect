package com.govconnect.auth.service;

import com.govconnect.auth.entity.User;
import com.govconnect.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link UserDetailsService} que busca usuarios
 * en la base de datos a través de {@link UserRepository}.
 * <p>
 * Carga al usuario por {@code username} y lanza
 * {@link UsernameNotFoundException} si no existe.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.debug("Usuario no encontrado: '{}'", username);
                    return new UsernameNotFoundException(
                            "Usuario no encontrado: " + username
                    );
                });

        log.debug("Usuario cargado: '{}'", username);
        return user;
    }
}
