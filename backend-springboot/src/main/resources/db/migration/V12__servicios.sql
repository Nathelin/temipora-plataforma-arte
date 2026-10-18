-- Servicios de artistas (UC-05/06/07/08). Normaliza la tabla y agrega los datos del alta.

-- Normalizar la columna "precio base" -> precio_base
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='public' AND table_name='servicio' AND column_name='precio base')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='public' AND table_name='servicio' AND column_name='precio_base') THEN
        ALTER TABLE servicio RENAME COLUMN "precio base" TO precio_base;
    END IF;
END $$;

-- Asegurar tabla base (por si el entorno está limpio)
CREATE TABLE IF NOT EXISTS servicio (
    id_servicio BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    precio_base NUMERIC(12,2),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE servicio ADD COLUMN IF NOT EXISTS id_artista BIGINT;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS id_tipo_servicio BIGINT;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS id_medio BIGINT;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS contenido_sensible VARCHAR(20);
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS tiempo_entrega_valor INTEGER;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS tiempo_entrega_unidad VARCHAR(10);
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS tiempo_entrega_dias INTEGER;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS cupos INTEGER;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS habilitar_cupos_tras_entrega BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS imagen_portada VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_artista') THEN
        ALTER TABLE servicio ADD CONSTRAINT fk_servicio_artista
            FOREIGN KEY (id_artista) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_tipo') THEN
        ALTER TABLE servicio ADD CONSTRAINT fk_servicio_tipo
            FOREIGN KEY (id_tipo_servicio) REFERENCES tipo_servicio (id_tipo_servicio);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicio_medio') THEN
        ALTER TABLE servicio ADD CONSTRAINT fk_servicio_medio
            FOREIGN KEY (id_medio) REFERENCES medio (id_medio);
    END IF;
END $$;

-- Imágenes de ejemplo del servicio
CREATE TABLE IF NOT EXISTS servicio_imagen (
    id_servicio_imagen BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_servicioimagen_servicio') THEN
        ALTER TABLE servicio_imagen ADD CONSTRAINT fk_servicioimagen_servicio
            FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio) ON DELETE CASCADE;
    END IF;
END $$;
