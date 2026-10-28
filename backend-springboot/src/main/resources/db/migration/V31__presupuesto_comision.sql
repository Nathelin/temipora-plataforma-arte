-- Comisión congelada en cada presupuesto.
-- El cliente paga el precio final sin cambios; la comisión se descuenta del
-- importe que recibe el artista. Se guarda el porcentaje aplicado y los montos
-- en moneda nativa y en la moneda de cobro, igual que la cotización.

ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS comision_porcentaje NUMERIC(5,2);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS comision_monto NUMERIC(12,2);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS comision_monto_cobro NUMERIC(12,2);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS monto_neto_artista NUMERIC(12,2);
ALTER TABLE presupuesto ADD COLUMN IF NOT EXISTS monto_neto_artista_cobro NUMERIC(12,2);

-- Backfill de presupuestos ya emitidos: se usa la comisión vigente en la base.
UPDATE presupuesto p
SET comision_porcentaje = COALESCE(
    (SELECT c.porcentaje_servicio
     FROM configuracion_comision c
     WHERE c.id_configuracion = 1),
    10.00
)
WHERE p.comision_porcentaje IS NULL;

UPDATE presupuesto
SET comision_monto = ROUND(precio_final * comision_porcentaje / 100, 2)
WHERE comision_monto IS NULL;

UPDATE presupuesto
SET comision_monto_cobro = ROUND(precio_final_cobro * comision_porcentaje / 100, 2)
WHERE comision_monto_cobro IS NULL;

UPDATE presupuesto
SET monto_neto_artista = precio_final - comision_monto
WHERE monto_neto_artista IS NULL;

UPDATE presupuesto
SET monto_neto_artista_cobro = precio_final_cobro - comision_monto_cobro
WHERE monto_neto_artista_cobro IS NULL;

ALTER TABLE presupuesto ALTER COLUMN comision_porcentaje SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN comision_monto SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN comision_monto_cobro SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN monto_neto_artista SET NOT NULL;
ALTER TABLE presupuesto ALTER COLUMN monto_neto_artista_cobro SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_comision_porcentaje'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_comision_porcentaje
            CHECK (comision_porcentaje BETWEEN 0 AND 100);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_comision_monto'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_comision_monto
            CHECK (comision_monto >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_presupuesto_monto_neto_artista'
    ) THEN
        ALTER TABLE presupuesto ADD CONSTRAINT ck_presupuesto_monto_neto_artista
            CHECK (monto_neto_artista >= 0);
    END IF;
END $$;
