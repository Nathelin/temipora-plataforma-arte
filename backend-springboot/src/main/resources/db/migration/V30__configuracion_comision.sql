-- Comisión de la plataforma: porcentajes independientes para servicios y productos.
-- El valor se congela en cada presupuesto al emitirse; cambiar la configuración
-- no altera presupuestos ya enviados ni ventas ya cerradas.

CREATE TABLE IF NOT EXISTS configuracion_comision (
    id_configuracion BIGINT PRIMARY KEY,
    porcentaje_servicio NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    porcentaje_producto NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_comision (id_configuracion, porcentaje_servicio, porcentaje_producto)
SELECT 1, 10.00, 10.00
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_comision WHERE id_configuracion = 1
);

ALTER TABLE configuracion_comision
    DROP CONSTRAINT IF EXISTS ck_configuracion_comision_rango;
ALTER TABLE configuracion_comision
    ADD CONSTRAINT ck_configuracion_comision_rango
    CHECK (porcentaje_servicio BETWEEN 0 AND 50
       AND porcentaje_producto BETWEEN 0 AND 50);
