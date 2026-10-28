-- Configuración global del plan de hitos: límites y plazos.
-- Los presupuestos ya emitidos no se ven afectados; solo aplica a los nuevos.

CREATE TABLE IF NOT EXISTS configuracion_pedido (
    id_configuracion BIGINT PRIMARY KEY,
    dias_aprobacion_automatica INTEGER NOT NULL DEFAULT 7,
    dias_pago_cuota INTEGER NOT NULL DEFAULT 7,
    max_hitos INTEGER NOT NULL DEFAULT 4,
    porcentaje_max_primer_hito NUMERIC(5,2) NOT NULL DEFAULT 50.00,
    monto_minimo_cuota NUMERIC(12,2) NOT NULL DEFAULT 1000.00,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_pedido (
    id_configuracion, dias_aprobacion_automatica, dias_pago_cuota,
    max_hitos, porcentaje_max_primer_hito, monto_minimo_cuota
)
SELECT 1, 7, 7, 4, 50.00, 1000.00
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_pedido WHERE id_configuracion = 1
);

ALTER TABLE configuracion_pedido
    DROP CONSTRAINT IF EXISTS ck_configuracion_pedido_rangos;
ALTER TABLE configuracion_pedido
    ADD CONSTRAINT ck_configuracion_pedido_rangos
    CHECK (dias_aprobacion_automatica BETWEEN 1 AND 60
       AND dias_pago_cuota BETWEEN 1 AND 90
       AND max_hitos BETWEEN 1 AND 6
       AND porcentaje_max_primer_hito BETWEEN 1 AND 100
       AND monto_minimo_cuota >= 0);
