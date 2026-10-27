-- UC-18: cobro de presupuestos en ARS.
-- El artista sigue cargando el precio en su moneda, pero el importe a pagar
-- queda expresado en ARS con la cotización vigente al momento de emitir.
-- Si la cotización cambia después, el presupuesto mantiene su importe.

ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS id_moneda_cobro BIGINT;
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS cotizacion_cobro NUMERIC(18,8);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS precio_base_total_cobro NUMERIC(12,2);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS precio_final_cobro NUMERIC(12,2);

-- Moneda de cobro: ARS (sembrada en V15).
UPDATE presupuesto p
SET id_moneda_cobro = (SELECT m.id_moneda FROM moneda m WHERE m.codigo = 'ARS' LIMIT 1)
WHERE p.id_moneda_cobro IS NULL;

-- Cotización histórica al momento de emisión. Se busca primero la conversión
-- directa y, si no existe, la inversa. Para presupuestos ya emitidos en ARS
-- el factor es 1. Si no hay historial, se deja 1 para no bloquear la migración.
UPDATE presupuesto p
SET cotizacion_cobro = COALESCE(
    (
        SELECT c.factor
        FROM conversion_moneda c
        WHERE c.id_moneda_entrada = p.id_moneda
          AND c.id_moneda_salida = p.id_moneda_cobro
          AND p.fecha_emision >= c.inicio_validez
          AND (c.fin_validez IS NULL OR p.fecha_emision < c.fin_validez)
        ORDER BY c.inicio_validez DESC
        LIMIT 1
    ),
    (
        SELECT 1 / c.factor
        FROM conversion_moneda c
        WHERE c.id_moneda_entrada = p.id_moneda_cobro
          AND c.id_moneda_salida = p.id_moneda
          AND p.fecha_emision >= c.inicio_validez
          AND (c.fin_validez IS NULL OR p.fecha_emision < c.fin_validez)
        ORDER BY c.inicio_validez DESC
        LIMIT 1
    ),
    CASE WHEN p.id_moneda = p.id_moneda_cobro THEN 1 ELSE NULL END
)
WHERE p.cotizacion_cobro IS NULL;

UPDATE presupuesto SET cotizacion_cobro = 1 WHERE cotizacion_cobro IS NULL;

UPDATE presupuesto
SET precio_base_total_cobro = ROUND(precio_base_total * cotizacion_cobro, 2)
WHERE precio_base_total_cobro IS NULL;

UPDATE presupuesto
SET precio_final_cobro = ROUND(precio_final * cotizacion_cobro, 2)
WHERE precio_final_cobro IS NULL;

ALTER TABLE presupuesto ALTER COLUMN id_moneda_cobro SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN cotizacion_cobro SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN precio_base_total_cobro SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN precio_final_cobro SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_presupuesto_moneda_cobro'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT fk_presupuesto_moneda_cobro
            FOREIGN KEY (id_moneda_cobro) REFERENCES moneda (id_moneda);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_cotizacion_cobro'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_cotizacion_cobro
            CHECK (cotizacion_cobro > 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_precio_base_total_cobro'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_precio_base_total_cobro
            CHECK (precio_base_total_cobro >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_precio_final_cobro'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_precio_final_cobro
            CHECK (precio_final_cobro >= 0);
    END IF;
END $$;
