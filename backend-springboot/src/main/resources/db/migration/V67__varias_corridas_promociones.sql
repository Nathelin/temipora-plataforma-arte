-- Permite repetir la corrida programada varias veces al día (modo pruebas).
ALTER TABLE configuracion_inactividad
    ADD COLUMN IF NOT EXISTS permitir_varias_corridas BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE configuracion_inactividad
    ADD COLUMN IF NOT EXISTS minutos_entre_corridas INTEGER NOT NULL DEFAULT 60;

ALTER TABLE configuracion_inactividad
    DROP CONSTRAINT IF EXISTS ck_configuracion_inactividad_minutos_entre_corridas;
ALTER TABLE configuracion_inactividad
    ADD CONSTRAINT ck_configuracion_inactividad_minutos_entre_corridas
    CHECK (minutos_entre_corridas > 0);
