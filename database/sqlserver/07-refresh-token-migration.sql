-- ==========================================
-- Migración: Refresh Token (token_version)
-- ==========================================
-- Agrega la columna token_version a la tabla users
-- para invalidación server-side de refresh tokens.
--
-- Cada vez que un usuario cierra sesión, token_version
-- se incrementa, rechazando cualquier refresh token
-- emitido con una versión anterior.
-- ==========================================

-- 1. Agregar columna token_version si no existe
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE name = 'token_version'
      AND object_id = OBJECT_ID(N'dbo.users')
)
BEGIN
    ALTER TABLE users
        ADD token_version INT NOT NULL DEFAULT 0;
END
GO

-- 2. Verificación
SELECT
    username,
    role,
    active,
    token_version
FROM users;
GO
