-- Términos del artista versionados e inmutables.
--
-- Antes: V9 creaba índices únicos por alcance (un solo global/servicio/producto
-- por artista). Eso impedía guardar una versión nueva sin pisar la anterior.
-- Ahora: cada edición inserta una fila nueva; sólo una queda `vigente`.
--
-- Las solicitudes guardan `id_terminos` + snapshot, así que las versiones
-- anteriores deben conservarse para respetar lo que cada usuario aceptó.

ALTER TABLE terminos ADD COLUMN IF NOT EXISTS vigente BOOLEAN NOT NULL DEFAULT TRUE;

-- Reemplaza los índices "un solo registro por alcance" de V9.
DROP INDEX IF EXISTS uk_terminos_artista_global;
DROP INDEX IF EXISTS uk_terminos_servicio;
DROP INDEX IF EXISTS uk_terminos_producto;

-- FK explícita al servicio (V9 sólo tenía el índice único, sin FK).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminos_servicio'
    ) THEN
        ALTER TABLE terminos
            ADD CONSTRAINT fk_terminos_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio);
    END IF;
END $$;

-- Una fila por alcance + versión.
CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_global_version
    ON terminos (id_artista, version)
    WHERE id_servicio IS NULL AND id_producto IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_servicio_version
    ON terminos (id_artista, id_servicio, version)
    WHERE id_servicio IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_producto_version
    ON terminos (id_producto, version)
    WHERE id_producto IS NOT NULL;

-- Una sola versión vigente por alcance.
CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_global_vigente
    ON terminos (id_artista)
    WHERE id_servicio IS NULL AND id_producto IS NULL AND vigente;

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_servicio_vigente
    ON terminos (id_artista, id_servicio)
    WHERE id_servicio IS NOT NULL AND vigente;

CREATE UNIQUE INDEX IF NOT EXISTS uk_terminos_producto_vigente
    ON terminos (id_producto)
    WHERE id_producto IS NOT NULL AND vigente;

-- Lecturas por alcance.
CREATE INDEX IF NOT EXISTS idx_terminos_servicio
    ON terminos (id_servicio, version DESC);

CREATE INDEX IF NOT EXISTS idx_terminos_artista
    ON terminos (id_artista, id_servicio, vigente);
