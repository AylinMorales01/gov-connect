-- ==========================================
-- Agregar columna role
-- Ejecutar solo si la columna no existe aún.
-- ==========================================
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE name = 'role'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    ALTER TABLE users
        ADD role VARCHAR(20) NOT NULL DEFAULT 'USER';
END
GO

-- ==========================================
-- Usuarios de prueba para desarrollo
-- ==========================================
-- ⚠️ SOLO PARA DESARROLLO — NO USAR EN PRODUCCIÓN ⚠️
--
-- Ambos usuarios usan la contraseña "password" (BCrypt 10 rounds).
-- En producción:
--   1. Eliminar estos INSERTs.
--   2. Crear usuarios reales con contraseñas seguras.
--   3. El admin debe cambiar su contraseña en el primer inicio de sesión.
--   4. Nunca usar contraseñas por defecto en entornos expuestos.
-- ==========================================

-- Usuario ADMIN de prueba
-- password: "password" (BCrypt)
IF NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, role, active)
    VALUES (
        'admin',
        'admin@govconnect.com',
        -- BCrypt hash de "password" (10 rounds)
        '$2a$10$ZJFMhy0DqVO2BEj/Dhf2teZJG8vk/O1yVFHPeHqcxjN9xtirrg6C.',
        'Administrador del Sistema',
        'ADMIN',
        1
    );
END
GO

-- Usuario USER de prueba
-- password: "password" (BCrypt)
-- Permite probar el flujo de autorización:
--   - Puede acceder a /analytics, /contracts, /automation
--   - NO puede acceder a /dashboard (reservado para ADMIN)
--   - ProtectedRoute redirige a /forbidden si intenta /dashboard
IF NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'usuario'
)
BEGIN
    INSERT INTO users (username, email, password_hash, full_name, role, active)
    VALUES (
        'usuario',
        'usuario@govconnect.com',
        -- BCrypt hash de "password" (10 rounds)
        '$2a$10$ZJFMhy0DqVO2BEj/Dhf2teZJG8vk/O1yVFHPeHqcxjN9xtirrg6C.',
        'Usuario de Prueba',
        'USER',
        1
    );
END
GO

-- ==========================================
-- Verificación de usuarios de prueba
-- ==========================================
SELECT username, role, active FROM users WHERE username IN ('admin', 'usuario');
GO
