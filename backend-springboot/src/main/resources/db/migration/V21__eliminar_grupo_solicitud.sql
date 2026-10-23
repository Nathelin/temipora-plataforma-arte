-- La solicitud ya no se divide: el grupo 1:N dejo de tener sentido.
-- La aceptacion de terminos y la prioridad pasan a la propia solicitud.

ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS id_terminos BIGINT;
ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS terminos_version INTEGER;
ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS terminos_aceptados_en TIMESTAMP;
ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS terminos_snapshot TEXT;
ALTER TABLE solicitud ADD COLUMN IF NOT EXISTS prioridad_definida_por_cliente BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE solicitud s
SET id_terminos = g.id_terminos,
    terminos_version = g.terminos_version,
    terminos_aceptados_en = g.terminos_aceptados_en,
    terminos_snapshot = g.terminos_snapshot,
    prioridad_definida_por_cliente = g.prioridad_definida_por_cliente
FROM grupo_solicitud g
WHERE s.id_grupo_solicitud = g.id_grupo_solicitud;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_solicitud_terminos') THEN
        ALTER TABLE solicitud ADD CONSTRAINT fk_solicitud_terminos
            FOREIGN KEY (id_terminos) REFERENCES terminos (id_terminos) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE solicitud ALTER COLUMN terminos_version SET NOT NULL;
ALTER TABLE solicitud ALTER COLUMN terminos_aceptados_en SET NOT NULL;
ALTER TABLE solicitud ALTER COLUMN terminos_snapshot SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_terminos ON solicitud (id_terminos);

ALTER TABLE solicitud DROP CONSTRAINT IF EXISTS fk_solicitud_grupo;
DROP INDEX IF EXISTS idx_solicitud_grupo;
ALTER TABLE solicitud DROP COLUMN IF EXISTS id_grupo_solicitud;
DROP TABLE IF EXISTS grupo_solicitud;
