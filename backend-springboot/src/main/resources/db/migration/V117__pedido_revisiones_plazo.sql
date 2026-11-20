-- Revisiones acotadas y plazo para reentregar después de una revisión.
-- Los pedidos ya creados arrancan con cero revisiones y sin plazo de reentrega.

ALTER TABLE hito_pedido
    ADD COLUMN IF NOT EXISTS revisiones INTEGER NOT NULL DEFAULT 0;

ALTER TABLE hito_pedido
    ADD COLUMN IF NOT EXISTS fecha_limite_reentrega TIMESTAMP;

ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS revisiones_maximas_por_hito INTEGER NOT NULL DEFAULT 2;

ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_reentrega INTEGER NOT NULL DEFAULT 7;

ALTER TABLE configuracion_pedido
    DROP CONSTRAINT IF EXISTS ck_configuracion_pedido_rangos;

ALTER TABLE configuracion_pedido
    ADD CONSTRAINT ck_configuracion_pedido_rangos
    CHECK (dias_aprobacion_automatica BETWEEN 1 AND 60
       AND dias_pago_cuota BETWEEN 1 AND 14
       AND max_hitos BETWEEN 1 AND 6
       AND porcentaje_max_primer_hito BETWEEN 1 AND 100
       AND monto_minimo_cuota >= 0
       AND revisiones_maximas_por_hito BETWEEN 1 AND 10
       AND dias_reentrega BETWEEN 1 AND 60);
