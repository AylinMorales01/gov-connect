USE SIA_CONNECT_DB;
GO

-- ==========================================
-- 1. Insertar 5 Departamentos
-- ==========================================
INSERT INTO departments (code, name, description) VALUES
   ('DEP-001', 'Tecnología', 'Departamento de TI y Desarrollo de Software'),
   ('DEP-002', 'Finanzas', 'Departamento Financiero, Contable y de Recaudo'),
   ('DEP-003', 'Recursos Humanos', 'Gestión de Talento y Bienestar'),
   ('DEP-004', 'Operaciones', 'Operaciones, Logística y Mantenimiento'),
   ('DEP-005', 'Legal', 'Asesoría Jurídica y Contratación');
GO

-- ==========================================
-- 2. Insertar 3 Usuarios (El equipo principal)
-- ==========================================
-- Contraseñas de ejemplo con (Bcrypt)
INSERT INTO users (name, email, password, role) VALUES
    ('Aylin', 'aylin@gmail.com', '$2a$10$wN9...', 'ADMIN'),
    ('Juan Camilo', 'camilo@gmail.com', '$2a$10$xyz...', 'MANAGER'),
    ('Ruben', 'ruben@gmail.com', '$2a$10$abc...', 'OPERATOR');
GO

-- ==========================================
-- 3. Insertar 5 Presupuestos
-- ==========================================
-- Cada departamento recibe un presupuesto para el año fiscal 2026
INSERT INTO budgets (department_id, fiscal_year, assigned_budget, executed_budget, available_budget) VALUES
   (1, 2026, 15000000.00, 5000000.00, 10000000.00),
   (2, 2026, 8000000.00, 4000000.00, 4000000.00),
   (3, 2026, 3500000.00, 3000000.00, 500000.00),
   (4, 2026, 25000000.00, 20000000.00, 5000000.00),
   (5, 2026, 4000000.00, 1000000.00, 3000000.00);
GO

-- ==========================================
-- 4. Insertar 20 Contratos
-- ==========================================
-- Algunos están activos, otros completados, y algunos expiran en los próximos 30 días para (agosto 2026)
INSERT INTO contracts (contract_number, contractor_name, object, contract_value, start_date, end_date, status, department_id) VALUES
    ('CT-2026-001', 'TechSolutions S.A.', 'Soporte de servidores en la nube', 4500000.00, '2026-01-15', '2026-12-31', 'ACTIVE', 1),
    ('CT-2026-002', 'LimpiaCorp', 'Servicios de aseo y cafetería', 1200000.00, '2026-02-01', '2026-08-10', 'ACTIVE', 4), -- Expira pronto
    ('CT-2026-003', 'Abogados Asociados', 'Consultoría legal externa', 3000000.00, '2026-03-01', '2026-09-01', 'ACTIVE', 5),
    ('CT-2026-004', 'Auditoría Total', 'Auditoría financiera anual', 8500000.00, '2026-04-10', '2026-06-10', 'COMPLETED', 2),
    ('CT-2026-005', 'ReclutaYa', 'Plataforma de selección de personal', 2500000.00, '2026-01-01', '2026-12-31', 'ACTIVE', 3),
    ('CT-2026-006', 'DevAcademy', 'Capacitación en Spring Boot', 1500000.00, '2026-07-01', '2026-08-15', 'ACTIVE', 1), -- Expira pronto
    ('CT-2026-007', 'Seguridad Máxima', 'Vigilancia 24/7', 9000000.00, '2026-01-01', '2026-12-31', 'ACTIVE', 4),
    ('CT-2026-008', 'Papelería Central', 'Suministro de oficina', 800000.00, '2026-05-01', '2026-11-30', 'ACTIVE', 4),
    ('CT-2026-009', 'CloudHost', 'Hosting de base de datos', 5200000.00, '2026-02-15', '2026-08-05', 'ACTIVE', 1), -- Expira pronto
    ('CT-2026-010', 'Consultores RH', 'Estudio de clima laboral', 4000000.00, '2026-03-01', '2026-05-30', 'COMPLETED', 3),
    ('CT-2026-011', 'Transito Seguro', 'Mantenimiento flota vehicular', 6500000.00, '2026-01-10', '2026-12-10', 'ACTIVE', 4),
    ('CT-2026-012', 'Licencias MS', 'Renovación Office 365', 3200000.00, '2026-06-01', '2027-06-01', 'ACTIVE', 1),
    ('CT-2026-013', 'Finanzas Pro', 'Software de contabilidad', 2100000.00, '2026-01-01', '2026-12-31', 'ACTIVE', 2),
    ('CT-2026-014', 'LegalTech', 'Acceso a jurisprudencia', 1100000.00, '2026-04-15', '2027-04-15', 'ACTIVE', 5),
    ('CT-2026-015', 'Salud y Vida', 'Exámenes médicos ocupacionales', 2800000.00, '2026-07-01', '2026-08-20', 'ACTIVE', 3), -- Expira pronto
    ('CT-2026-016', 'Mundo PC', 'Compra de 10 laptops', 25000000.00, '2026-02-01', '2026-03-01', 'COMPLETED', 1),
    ('CT-2026-017', 'Logística Express', 'Servicio de mensajería', 950000.00, '2026-01-01', '2026-12-31', 'ACTIVE', 4),
    ('CT-2026-018', 'Eventos Empresariales', 'Fiesta de fin de año', 5000000.00, '2026-12-01', '2026-12-31', 'SUSPENDED', 3),
    ('CT-2026-019', 'Redes Plus', 'Cableado estructurado', 3800000.00, '2026-05-10', '2026-07-10', 'COMPLETED', 1),
    ('CT-2026-020', 'Asesores Tributarios', 'Declaración de renta', 4500000.00, '2026-07-15', '2026-08-30', 'ACTIVE', 2);
GO

-- ==========================================
-- 5. Insertar 100 Recaudos (Ciclo automático)
-- ==========================================
DECLARE @contador INT = 1;
DECLARE @departamento_random INT;
DECLARE @metodo_pago VARCHAR(50);

WHILE @contador <= 100
    BEGIN
        SET @departamento_random = (@contador % 5) + 1; -- Asigna IDs del 1 al 5

        IF @contador % 3 = 0 SET @metodo_pago = 'CREDIT_CARD';
        ELSE IF @contador % 2 = 0 SET @metodo_pago = 'BANK_TRANSFER';
        ELSE SET @metodo_pago = 'CASH';

        INSERT INTO collections (collection_date, concept, taxpayer, amount, payment_method, department_id)
        VALUES (
                   DATEADD(DAY, -(@contador % 22), '2026-07-23'), -- Distribuye las fechas en los últimos 22 días de julio
                   'Pago de Impuesto / Multa #' + CAST(@contador AS VARCHAR),
                   'Contribuyente ' + CAST(@contador AS VARCHAR),
                   (50000.00 * (@contador % 4 + 1)) + 12500.00, -- Valores variados
                   @metodo_pago,
                   @departamento_random
               );

        SET @contador = @contador + 1;
    END;
GO

-- ==========================================
-- 6. Insertar 10 Logs de Automatización
-- ==========================================
INSERT INTO automation_logs (user_id, process, status, message, execution_time_ms) VALUES
    (1, 'SYNC_ERP_INVENTORY', 'SUCCESS', 'Sincronización de ERP completada correctamente', 1250),
    (2, 'GENERATE_MONTHLY_REPORT', 'SUCCESS', 'Reporte financiero generado en PDF', 3400),
    (3, 'UPDATE_DASHBOARD_CACHE', 'FAILED', 'Timeout conectando a la base de datos', 5000),
    (1, 'SEND_NOTIFICATIONS', 'SUCCESS', 'Notificaciones de vencimiento enviadas', 850),
    (1, 'SYNC_ERP_INVENTORY', 'SUCCESS', 'Sincronización de ERP completada correctamente', 1100),
    (2, 'DATA_BACKUP', 'SUCCESS', 'Backup automático creado en S3', 15000),
    (3, 'UPDATE_DASHBOARD_CACHE', 'SUCCESS', 'Caché del dashboard actualizado', 420),
    (2, 'GENERATE_MONTHLY_REPORT', 'SUCCESS', 'Reporte de recursos humanos generado', 3300),
    (1, 'SYNC_ERP_INVENTORY', 'SUCCESS', 'Sincronización de ERP completada correctamente', 1200),
    (3, 'SEND_NOTIFICATIONS', 'FAILED', 'Error de servicio SMTP', 450);
GO