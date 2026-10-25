-- Distingue las acciones tomadas por el artista de las producidas por la baja definitiva
-- del servicio (UC-07), sin cambiar el estado que ve el cliente.

ALTER TABLE accion_solicitud
    ADD COLUMN IF NOT EXISTS origen VARCHAR(20);

UPDATE accion_solicitud SET origen = 'ARTISTA' WHERE origen IS NULL;

ALTER TABLE accion_solicitud ALTER COLUMN origen SET DEFAULT 'ARTISTA';
ALTER TABLE accion_solicitud ALTER COLUMN origen SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_accion_solicitud_origen') THEN
        ALTER TABLE accion_solicitud ADD CONSTRAINT ck_accion_solicitud_origen
            CHECK (origen IN ('ARTISTA', 'BAJA_SERVICIO'));
    END IF;
END $$;
