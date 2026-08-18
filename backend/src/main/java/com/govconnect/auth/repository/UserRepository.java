package com.govconnect.auth.repository;

import com.govconnect.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su nombre de usuario (username).
     *
     * @param username nombre de usuario único.
     * @return {@link Optional} con el usuario si existe.
     */
    Optional<User> findByUsername(String username);

    /**
     * Verifica si ya existe un usuario con el username dado.
     * <p>
     * <b>Uso previsto:</b> endpoint de registro de usuarios (POST /auth/register)
     * para evitar duplicados de username antes de crear la cuenta.
     * No se elimina para que esté disponible cuando se implemente el registro.
     * </p>
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si ya existe un usuario con el email dado.
     * <p>
     * <b>Uso previsto:</b> endpoint de registro de usuarios (POST /auth/register)
     * para evitar duplicados de email antes de crear la cuenta.
     * No se elimina para que esté disponible cuando se implemente el registro.
     * </p>
     */
    boolean existsByEmail(String email);
}
