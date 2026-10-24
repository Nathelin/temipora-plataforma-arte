-- Ciclo de vida del servicio (UC-07): publicado / pausado / archivado.
-- Reemplaza el flag booleano "estado" por un estado explícito y agrega la auditoría de la baja.

DO $$
DECLARE
    tipo_actual TEXT;
BEGIN
    SELECT data_type INTO tipo_actual
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'servicio'
       AND column_name = 'estado';

    IF tipo_actual = 'boolean' THEN
        ALTER TABLE servicio ADD COLUMN IF NOT EXISTS estado_ciclo VARCHAR(20);
        UPDATE servicio
           SET estado_ciclo = CASE WHEN estado THEN 'PUBLICADO' ELSE 'PAUSADO' END;
        ALTER TABLE servicio DROP COLUMN estado;
        ALTER TABLE servicio RENAME COLUMN estado_ciclo TO estado;
    ELSIF tipo_actual IS NULL THEN
        ALTER TABLE servicio ADD COLUMN estado VARCHAR(20);
    END IF;
END $$;

UPDATE servicio SET estado = 'PUBLICADO' WHERE estado IS NULL;

ALTER TABLE servicio ALTER COLUMN estado SET DEFAULT 'PUBLICADO';
ALTER TABLE servicio ALTER COLUMN estado SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_servicio_estado') THEN
        ALTER TABLE servicio ADD CONSTRAINT ck_servicio_estado
            CHECK (estado IN ('PUBLICADO', 'PAUSADO', 'ARCHIVADO'));
    END IF;
END $$;

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS pausado_en TIMESTAMP;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS archivado_en TIMESTAMP;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS id_archivado_por BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_archivado_por') THEN
        ALTER TABLE servicio ADD CONSTRAINT fk_servicio_archivado_por
            FOREIGN KEY (id_archivado_por) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_servicio_artista_estado
    ON servicio (id_artista, estado);
