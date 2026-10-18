-- Servicios: contenido sensible pasa a ser flag booleano + tabla de tipos que aplican.

CREATE TABLE IF NOT EXISTS servicio_contenido_sensible (
    id_servicio BIGINT NOT NULL,
    contenido VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_servicio, contenido)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_serviciocontenido_servicio') THEN
        ALTER TABLE servicio_contenido_sensible ADD CONSTRAINT fk_serviciocontenido_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
END $$;

-- Migrar datos existentes: si el servicio tenía un tipo marcado, el flag pasa a TRUE
-- y el tipo se copia a la tabla nueva. Luego la columna pasa a boolean.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='public' AND table_name='servicio' AND column_name='contenido_sensible')
       AND (SELECT data_type FROM information_schema.columns
            WHERE table_schema='public' AND table_name='servicio' AND column_name='contenido_sensible') = 'character varying' THEN

        INSERT INTO servicio_contenido_sensible (id_servicio, contenido)
        SELECT id_servicio, contenido_sensible FROM servicio
        WHERE contenido_sensible IS NOT NULL
        ON CONFLICT DO NOTHING;

        ALTER TABLE servicio DROP COLUMN contenido_sensible;
        ALTER TABLE servicio ADD COLUMN contenido_sensible BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;
