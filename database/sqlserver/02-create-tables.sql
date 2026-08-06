-- ==========================================
-- Tabla: departments
-- ==========================================
IF OBJECT_ID(N'dbo.departments', N'U') IS NULL
BEGIN
    CREATE TABLE departments (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        code        VARCHAR(10)  NOT NULL UNIQUE,
        name        VARCHAR(100) NOT NULL,
        description VARCHAR(255),
        active      BIT          NOT NULL DEFAULT 1,
        created_at  DATETIME2    NOT NULL DEFAULT GETDATE(),
        updated_at  DATETIME2    NULL
    );
END
GO

-- ==========================================
-- Tabla: budgets
-- ==========================================
IF OBJECT_ID(N'dbo.budgets', N'U') IS NULL
BEGIN
    CREATE TABLE budgets (
        id               BIGINT IDENTITY(1,1) PRIMARY KEY,
        department_id    BIGINT         NOT NULL,
        fiscal_year      INT            NOT NULL,
        assigned_budget  DECIMAL(18,2)  NOT NULL,
        executed_budget  DECIMAL(18,2)  NOT NULL,
        available_budget DECIMAL(18,2)  NOT NULL,
        created_at       DATETIME2      NOT NULL DEFAULT GETDATE(),
        updated_at       DATETIME2      NULL
    );
END
GO

-- ==========================================
-- Tabla: contracts
-- ==========================================
IF OBJECT_ID(N'dbo.contracts', N'U') IS NULL
BEGIN
    CREATE TABLE contracts (
        id              BIGINT IDENTITY(1,1) PRIMARY KEY,
        contract_number VARCHAR(30)   NOT NULL UNIQUE,
        contractor_name VARCHAR(150)  NOT NULL,
        object          VARCHAR(255)  NOT NULL,
        contract_value  DECIMAL(18,2) NOT NULL,
        start_date      DATE          NOT NULL,
        end_date        DATE          NOT NULL,
        status          VARCHAR(20)   NOT NULL,
        department_id   BIGINT        NOT NULL,
        created_at      DATETIME2     NOT NULL DEFAULT GETDATE(),
        updated_at      DATETIME2     NULL
    );
END
GO

-- ==========================================
-- Tabla: collections
-- ==========================================
IF OBJECT_ID(N'dbo.collections', N'U') IS NULL
BEGIN
    CREATE TABLE collections (
        id              BIGINT IDENTITY(1,1) PRIMARY KEY,
        collection_date DATE          NOT NULL,
        concept         VARCHAR(100)  NOT NULL,
        taxpayer        VARCHAR(150),
        amount          DECIMAL(18,2) NOT NULL,
        payment_method  VARCHAR(50),
        department_id   BIGINT        NOT NULL,
        created_at      DATETIME2     NOT NULL DEFAULT GETDATE()
    );
END
GO