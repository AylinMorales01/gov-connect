SET NOCOUNT ON;

CREATE TABLE #MonthlyTargets (
                                 YearMonth VARCHAR(7),
                                 TargetAmount DECIMAL(18,2)
);

INSERT INTO #MonthlyTargets VALUES
                                ('2025-01', 120000000), ('2025-02', 118000000), ('2025-03', 126000000), ('2025-04', 131000000),
                                ('2025-05', 136000000), ('2025-06', 140000000), ('2025-07', 145000000), ('2025-08', 149000000),
                                ('2025-09', 151000000), ('2025-10', 157000000), ('2025-11', 162000000), ('2025-12', 174000000),
                                ('2026-01', 168000000), ('2026-02', 171000000), ('2026-03', 176000000), ('2026-04', 183000000),
                                ('2026-05', 190000000), ('2026-06', 194000000), ('2026-07', 201000000), ('2026-08', 206000000),
                                ('2026-09', 214000000), ('2026-10', 220000000), ('2026-11', 227000000), ('2026-12', 238000000);

DECLARE @CurrentMonth VARCHAR(7);
DECLARE @Target DECIMAL(18,2);
DECLARE @BaseAmount DECIMAL(18,2);
DECLARE @Date DATE;

DECLARE target_cursor CURSOR FOR SELECT YearMonth, TargetAmount FROM #MonthlyTargets;
OPEN target_cursor;

FETCH NEXT FROM target_cursor INTO @CurrentMonth, @Target;

WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @BaseAmount = @Target / 15;

        DECLARE @i INT = 1;
        DECLARE @Accumulated DECIMAL(18,2) = 0;
        DECLARE @InsertAmount DECIMAL(18,2);

        WHILE @i <= 15
            BEGIN
                IF @i = 15
                    SET @InsertAmount = @Target - @Accumulated;
                ELSE
                    SET @InsertAmount = @BaseAmount + ((RAND() * 2000000) - 1000000);

                SET @Accumulated = @Accumulated + @InsertAmount;
                SET @Date = CAST(@CurrentMonth + '-' + RIGHT('0' + CAST((@i) AS VARCHAR(2)), 2) AS DATE);

                DECLARE @Concept VARCHAR(100);
                DECLARE @Dept BIGINT;

                IF @i % 5 = 0 BEGIN SET @Concept = 'Industria y Comercio'; SET @Dept = 1; END
                ELSE IF @i % 4 = 0 BEGIN SET @Concept = 'Multas de Tránsito'; SET @Dept = 6; END
                ELSE IF @i % 3 = 0 BEGIN SET @Concept = 'Tasas Administrativas'; SET @Dept = 4; END
                ELSE IF @i % 2 = 0 BEGIN SET @Concept = 'Alumbrado Público'; SET @Dept = 5; END
                ELSE BEGIN SET @Concept = 'Impuesto Predial'; SET @Dept = 1; END

                INSERT INTO collections (collection_date, concept, taxpayer, amount, payment_method, department_id)
                VALUES (
                           @Date,
                           @Concept,
                           'Contribuyente ' + CAST(CAST(RAND() * 1000 AS INT) AS VARCHAR(10)),
                           @InsertAmount,
                           CASE WHEN @i % 2 = 0 THEN 'PSE' WHEN @i % 3 = 0 THEN 'Tarjeta de Crédito' ELSE 'Transferencia' END,
                           @Dept
                       );

                SET @i = @i + 1;
            END

        FETCH NEXT FROM target_cursor INTO @CurrentMonth, @Target;
    END

CLOSE target_cursor;
DEALLOCATE target_cursor;
DROP TABLE #MonthlyTargets;