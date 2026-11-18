-- Baja voluntaria de cuenta y plazo para reactivarla.
-- La columna queda en usuario; el parámetro global se administra desde el panel.

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS fecha_baja_voluntaria TIMESTAMP;

CREATE TABLE IF NOT EXISTS configuracion_baja_cuenta (
    id_configuracion BIGINT PRIMARY KEY,
    dias_para_reactivar INTEGER NOT NULL DEFAULT 30,
    fecha_actualizacion TIMESTAMP
);

INSERT INTO configuracion_baja_cuenta (id_configuracion, dias_para_reactivar)
SELECT 1, 30
WHERE NOT EXISTS (
    SELECT 1 FROM configuracion_baja_cuenta WHERE id_configuracion = 1
);

ALTER TABLE configuracion_baja_cuenta
    DROP CONSTRAINT IF EXISTS ck_configuracion_baja_cuenta_dias;
ALTER TABLE configuracion_baja_cuenta
    ADD CONSTRAINT ck_configuracion_baja_cuenta_dias
    CHECK (dias_para_reactivar BETWEEN 1 AND 365);

CREATE INDEX IF NOT EXISTS idx_usuario_estado_cuenta ON usuario (estado_cuenta);
