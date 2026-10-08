-- Módulo de materiales y herramientas + catálogo de tipos de arte (Opción B)
-- Reemplaza el enum imagen.medio por una FK a tipo_arte.

-- Catálogo de tipos de arte (alta/baja desde el panel de administración)
CREATE TABLE IF NOT EXISTS tipo_arte (
    id_tipo_arte BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250),
    medio_bucket VARCHAR(20) NOT NULL,
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_tipo_arte_nombre'
    ) THEN
        ALTER TABLE tipo_arte
            ADD CONSTRAINT uk_tipo_arte_nombre
            UNIQUE (nombre);
    END IF;
END $$;

-- Semilla: los dos tipos iniciales (idempotente)
INSERT INTO tipo_arte (nombre, descripcion, medio_bucket, activo, fecha_creacion)
SELECT v.nombre, v.descripcion, v.medio_bucket, TRUE, now()
FROM (VALUES
    ('Tradicional', 'Obras creadas con medios físicos (papel, lienzo, arcilla, ...)', 'TRADICIONAL'),
    ('Digital', 'Obras creadas con herramientas digitales (tablet, software, ...)', 'DIGITAL')
) AS v(nombre, descripcion, medio_bucket)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_arte t WHERE t.nombre = v.nombre
);

-- Materiales y herramientas (vocabulario controlado, alta directa del admin)
CREATE TABLE IF NOT EXISTS material (
    id_material BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250),
    id_tipo_arte BIGINT NOT NULL,
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_material_nombre'
    ) THEN
        ALTER TABLE material
            ADD CONSTRAINT uk_material_nombre
            UNIQUE (nombre);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_material_tipo_arte'
    ) THEN
        ALTER TABLE material
            ADD CONSTRAINT fk_material_tipo_arte
            FOREIGN KEY (id_tipo_arte) REFERENCES tipo_arte (id_tipo_arte);
    END IF;
END $$;

-- Intermedia imagen <-> material
CREATE TABLE IF NOT EXISTS imagen_material (
    id_imagen_material BIGSERIAL PRIMARY KEY,
    id_imagen BIGINT NOT NULL,
    id_material BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_imagen_material'
    ) THEN
        ALTER TABLE imagen_material
            ADD CONSTRAINT uk_imagen_material
            UNIQUE (id_imagen, id_material);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_imagenmaterial_imagen'
    ) THEN
        ALTER TABLE imagen_material
            ADD CONSTRAINT fk_imagenmaterial_imagen
            FOREIGN KEY (id_imagen) REFERENCES imagen (id_imagen);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_imagenmaterial_material'
    ) THEN
        ALTER TABLE imagen_material
            ADD CONSTRAINT fk_imagenmaterial_material
            FOREIGN KEY (id_material) REFERENCES material (id_material);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'imagen_material'
          AND indexname = 'idx_imagen_material_material'
    ) THEN
        CREATE INDEX idx_imagen_material_material
            ON imagen_material (id_material);
    END IF;
END $$;

-- imagen.medio (enum) -> imagen.id_tipo_arte (FK), con backfill de datos existentes
DO $$
BEGIN
    IF to_regclass('public.imagen') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'imagen'
          AND column_name = 'id_tipo_arte'
    ) THEN
        ALTER TABLE imagen ADD COLUMN id_tipo_arte BIGINT;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'imagen'
          AND column_name = 'medio'
    ) THEN
        UPDATE imagen i
        SET id_tipo_arte = (
            SELECT t.id_tipo_arte
            FROM tipo_arte t
            WHERE t.medio_bucket = i.medio
            ORDER BY t.id_tipo_arte
            LIMIT 1
        )
        WHERE i.id_tipo_arte IS NULL
          AND i.medio IS NOT NULL;

        ALTER TABLE imagen DROP COLUMN medio;
    END IF;

    ALTER TABLE imagen ALTER COLUMN id_tipo_arte SET NOT NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
            ON kcu.constraint_name = tc.constraint_name
           AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'imagen'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'id_tipo_arte'
    ) THEN
        ALTER TABLE imagen
            ADD CONSTRAINT fk_imagen_tipo_arte
            FOREIGN KEY (id_tipo_arte) REFERENCES tipo_arte (id_tipo_arte);
    END IF;
END $$;
