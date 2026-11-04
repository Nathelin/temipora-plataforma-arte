-- Se elimina el modo de varias corridas: alcanza con cambiar la hora de envío.
ALTER TABLE configuracion_inactividad
    DROP CONSTRAINT IF EXISTS ck_configuracion_inactividad_minutos_entre_corridas;

ALTER TABLE configuracion_inactividad
    DROP COLUMN IF EXISTS permitir_varias_corridas;

ALTER TABLE configuracion_inactividad
    DROP COLUMN IF EXISTS minutos_entre_corridas;
