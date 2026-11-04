-- Hora configurable de la corrida diaria de promociones.
ALTER TABLE configuracion_inactividad
    ADD COLUMN IF NOT EXISTS hora_ejecucion TIME NOT NULL DEFAULT '09:00';

ALTER TABLE configuracion_inactividad
    ADD COLUMN IF NOT EXISTS ultima_corrida_programada TIMESTAMP;
