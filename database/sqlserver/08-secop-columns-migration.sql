-- ==========================================
-- Migración: columnas de contracts para export de SECOP II
-- ==========================================
-- El export real de SECOP II ("Contratos Electrónicos") no cabe en el
-- dimensionamiento original de la tabla:
--
--   * "Objeto del Contrato" llega hasta 500 caracteres (9.803 de 13.545 filas
--     de un export de ejemplo superan los 255 originales).
--   * "Referencia del Contrato" llega hasta 40 caracteres, contra 30.
--
-- Un INSERT que excede el ancho no falla fila a fila: aborta la transacción
-- completa de la importación. Además se pasa a NVARCHAR porque los objetos
-- traen tildes y eñes, y el driver JDBC envía los parámetros como Unicode.
--
-- Idempotente: se puede ejecutar varias veces sin efecto adicional.
-- ==========================================

USE GOV_CONNECT_DB;
GO

-- 0. Asegurar el departamento de respaldo "Sin asignar", al que caen las
--    entidades del export que no calzan con las secretarías del seed.
IF NOT EXISTS (SELECT 1 FROM departments WHERE code = 'SIN')
BEGIN
    INSERT INTO departments (code, name, description) VALUES
        ('SIN', 'Sin asignar', 'Respaldo para entidades del export no reconocidas');
END
GO

-- 1. Soltar la restricción UNIQUE de contract_number (nombre autogenerado por
--    haberse declarado en línea en el CREATE TABLE) para poder alterar la columna
DECLARE @uniqueConstraint SYSNAME;

SELECT @uniqueConstraint = kc.name
FROM sys.key_constraints kc
    JOIN sys.index_columns ic
        ON ic.object_id = kc.parent_object_id
       AND ic.index_id = kc.unique_index_id
    JOIN sys.columns c
        ON c.object_id = ic.object_id
       AND c.column_id = ic.column_id
WHERE kc.parent_object_id = OBJECT_ID(N'dbo.contracts')
  AND kc.type = 'UQ'
  AND c.name = 'contract_number';

IF @uniqueConstraint IS NOT NULL
    EXEC('ALTER TABLE dbo.contracts DROP CONSTRAINT [' + @uniqueConstraint + ']');
GO

-- 2. Ampliar las columnas
ALTER TABLE dbo.contracts ALTER COLUMN contract_number NVARCHAR(50)  NOT NULL;
GO

ALTER TABLE dbo.contracts ALTER COLUMN contractor_name NVARCHAR(150) NOT NULL;
GO

ALTER TABLE dbo.contracts ALTER COLUMN object          NVARCHAR(500) NOT NULL;
GO

-- 3. Recrear la restricción UNIQUE, ahora con nombre explícito
IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = 'UQ_CONTRACT_NUMBER'
      AND parent_object_id = OBJECT_ID(N'dbo.contracts')
)
BEGIN
    ALTER TABLE dbo.contracts
        ADD CONSTRAINT UQ_CONTRACT_NUMBER UNIQUE (contract_number);
END
GO

-- 4. Verificación
SELECT
    c.name                AS columna,
    t.name                AS tipo,
    c.max_length / 2      AS caracteres   -- NVARCHAR almacena 2 bytes por carácter
FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
WHERE c.object_id = OBJECT_ID(N'dbo.contracts')
  AND c.name IN ('contract_number', 'contractor_name', 'object');
GO
