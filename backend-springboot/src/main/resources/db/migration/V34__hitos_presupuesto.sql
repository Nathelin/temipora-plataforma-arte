-- Plan de pagos del presupuesto (hitos).
-- Snapshot inmutable: se arma al emitir y no se modifica después.
-- El tipo se deriva de la posición (primera = seña, última = entrega).
-- Los presupuestos ya emitidos reciben un único hito de entrega al 100%.

CREATE TABLE IF NOT EXISTS hito_presupuesto (
    id_hito_presupuesto BIGSERIAL PRIMARY KEY,
    id_presupuesto BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    descripcion VARCHAR(250),
    tipo VARCHAR(10) NOT NULL,
    porcentaje NUMERIC(5,2),
    monto NUMERIC(12,2) NOT NULL,
    monto_cobro NUMERIC(12,2) NOT NULL,
    comision_monto NUMERIC(12,2) NOT NULL,
    comision_monto_cobro NUMERIC(12,2) NOT NULL,
    monto_neto NUMERIC(12,2) NOT NULL,
    monto_neto_cobro NUMERIC(12,2) NOT NULL,
    requiere_entrega BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_hito_presupuesto_presupuesto'
    ) THEN
        ALTER TABLE hito_presupuesto ADD CONSTRAINT fk_hito_presupuesto_presupuesto
            FOREIGN KEY (id_presupuesto)
            REFERENCES presupuesto (id_presupuesto) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_presupuesto_tipo'
    ) THEN
        ALTER TABLE hito_presupuesto ADD CONSTRAINT ck_hito_presupuesto_tipo
            CHECK (tipo IN ('SENA', 'CUOTA', 'ENTREGA'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_presupuesto_porcentaje'
    ) THEN
        ALTER TABLE hito_presupuesto ADD CONSTRAINT ck_hito_presupuesto_porcentaje
            CHECK (porcentaje IS NULL OR porcentaje BETWEEN 0.01 AND 100);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_hito_presupuesto_montos'
    ) THEN
        ALTER TABLE hito_presupuesto ADD CONSTRAINT ck_hito_presupuesto_montos
            CHECK (monto >= 0 AND monto_cobro >= 0
               AND comision_monto >= 0 AND comision_monto_cobro >= 0
               AND monto_neto >= 0 AND monto_neto_cobro >= 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_hito_presupuesto_orden
    ON hito_presupuesto (id_presupuesto, orden);
CREATE INDEX IF NOT EXISTS idx_hito_presupuesto_presupuesto
    ON hito_presupuesto (id_presupuesto);

INSERT INTO hito_presupuesto (
    id_presupuesto, orden, nombre, descripcion, tipo, porcentaje,
    monto, monto_cobro, comision_monto, comision_monto_cobro,
    monto_neto, monto_neto_cobro, requiere_entrega
)
SELECT
    p.id_presupuesto,
    0,
    'Entrega final',
    NULL,
    'ENTREGA',
    100.00,
    p.precio_final,
    p.precio_final_cobro,
    p.comision_monto,
    p.comision_monto_cobro,
    p.monto_neto_artista,
    p.monto_neto_artista_cobro,
    TRUE
FROM presupuesto p
WHERE NOT EXISTS (
    SELECT 1 FROM hito_presupuesto h
    WHERE h.id_presupuesto = p.id_presupuesto
);
