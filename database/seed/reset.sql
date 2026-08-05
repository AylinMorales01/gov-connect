-- database/seed/reset.sql

-- 1. Borrar tablas transaccionales (hijas)
DELETE FROM collections;
DELETE FROM contracts;
DELETE FROM budgets;

-- 2. Borrar tablas maestras (padres)
DELETE FROM departments;

-- Reiniciar los contadores de identidad (IDs automáticos) a 1
DBCC CHECKIDENT ('collections', RESEED, 0);
DBCC CHECKIDENT ('contracts', RESEED, 0);
DBCC CHECKIDENT ('budgets', RESEED, 0);
DBCC CHECKIDENT ('departments', RESEED, 0);