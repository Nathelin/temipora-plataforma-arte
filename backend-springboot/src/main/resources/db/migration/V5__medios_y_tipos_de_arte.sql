-- Medios como catálogo administrable (reemplaza al enum tipo_arte.medio_bucket)
-- + renombrado de las semillas de tipo de arte a "Dibujo tradicional" / "Dibujo digital".

-- Catálogo de medios (alta/baja desde el panel de administración)
CREATE TABLE IF NOT EXISTS medio (
    id_medio BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250),
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_medio_nombre'
    ) THEN
        ALTER TABLE medio
            ADD CONSTRAINT uk_medio_nombre
            UNIQUE (nombre);
    END IF;
END $$;

-- Semilla: los dos medios iniciales (idempotente)
INSERT INTO medio (nombre, descripcion, activo, fecha_creacion)
SELECT v.nombre, v.descripcion, TRUE, now()
FROM (VALUES
    ('Tradicional', 'Obras creadas con medios físicos (papel, lienzo, arcilla, ...)'),
    ('Digital', 'Obras creadas con herramientas digitales (tablet, software, ...)')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM medio m WHERE m.nombre = v.nombre
);

-- tipo_arte: medio_bucket (enum) -> id_medio (FK), con backfill de datos existentes
DO $$
BEGIN
    IF to_regclass('public.tipo_arte') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tipo_arte'
          AND column_name = 'id_medio'
    ) THEN
        ALTER TABLE tipo_arte ADD COLUMN id_medio BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tipo_arte'
          AND column_name = 'medio_bucket'
    ) THEN
        UPDATE tipo_arte t
        SET id_medio = m.id_medio
        FROM medio m
        WHERE t.id_medio IS NULL
          AND (
                (t.medio_bucket = 'TRADICIONAL' AND m.nombre = 'Tradicional')
             OR (t.medio_bucket = 'DIGITAL' AND m.nombre = 'Digital')
          );
    END IF;

    ALTER TABLE tipo_arte ALTER COLUMN id_medio SET NOT NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON kcu.constraint_name = tc.constraint_name
           AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'tipo_arte'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'id_medio'
    ) THEN
        ALTER TABLE tipo_arte
            ADD CONSTRAINT fk_tipo_arte_medio
            FOREIGN KEY (id_medio) REFERENCES medio (id_medio);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'tipo_arte'
          AND column_name = 'medio_bucket'
    ) THEN
        ALTER TABLE tipo_arte DROP COLUMN medio_bucket;
    END IF;
END $$;

-- Renombrado de semillas (idempotente: no rompe si ya se renombraron a mano)
UPDATE tipo_arte
SET nombre = 'Dibujo tradicional'
WHERE nombre = 'Tradicional'
  AND NOT EXISTS (
      SELECT 1 FROM tipo_arte x WHERE x.nombre = 'Dibujo tradicional'
  );

UPDATE tipo_arte
SET nombre = 'Dibujo digital'
WHERE nombre = 'Digital'
  AND NOT EXISTS (
      SELECT 1 FROM tipo_arte x WHERE x.nombre = 'Dibujo digital'
  );
