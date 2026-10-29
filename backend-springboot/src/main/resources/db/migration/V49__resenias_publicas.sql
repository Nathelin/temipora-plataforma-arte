-- Visibilidad pública de reseñas: anonimato, moderación e imágenes.
-- Las tablas de imágenes y reportes se crean aquí para no encadenar migraciones;
-- el código que las usa se incorpora en las fases siguientes.

ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS mostrar_nombre BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS moderada_en TIMESTAMP;
ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS motivo_moderacion VARCHAR(500);

ALTER TABLE resenia DROP CONSTRAINT IF EXISTS ck_resenia_estado;
ALTER TABLE resenia
    ADD CONSTRAINT ck_resenia_estado
    CHECK (estado IN ('PUBLICADA', 'ELIMINADA', 'OCULTA'));

ALTER TABLE configuracion_resenia
    ADD COLUMN IF NOT EXISTS max_imagenes_resenia INTEGER NOT NULL DEFAULT 4;

ALTER TABLE configuracion_resenia
    DROP CONSTRAINT IF EXISTS ck_configuracion_resenia_imagenes;
ALTER TABLE configuracion_resenia
    ADD CONSTRAINT ck_configuracion_resenia_imagenes
    CHECK (max_imagenes_resenia BETWEEN 0 AND 10);

-- Imágenes opcionales de la reseña, asociadas a la versión (historial incluido).
CREATE TABLE IF NOT EXISTS resenia_imagen (
    id_resenia_imagen BIGSERIAL PRIMARY KEY,
    id_version BIGINT NOT NULL,
    id_archivo BIGINT NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_imagen_version'
    ) THEN
        ALTER TABLE resenia_imagen
            ADD CONSTRAINT fk_resenia_imagen_version
            FOREIGN KEY (id_version) REFERENCES resenia_version (id_version)
            ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_resenia_imagen_archivo'
    ) THEN
        ALTER TABLE resenia_imagen
            ADD CONSTRAINT fk_resenia_imagen_archivo
            FOREIGN KEY (id_archivo) REFERENCES entrega_archivo (id_archivo);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resenia_imagen_version_archivo
    ON resenia_imagen (id_version, id_archivo);
CREATE INDEX IF NOT EXISTS idx_resenia_imagen_version
    ON resenia_imagen (id_version);

-- Reportes de reseñas para la moderación del administrador.
CREATE TABLE IF NOT EXISTS reporte_resenia (
    id_reporte BIGSERIAL PRIMARY KEY,
    id_resenia BIGINT NOT NULL,
    id_reportante BIGINT NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    comentario VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    id_resuelto_por BIGINT,
    fecha_resolucion TIMESTAMP,
    accion VARCHAR(20),
    CONSTRAINT ck_reporte_resenia_motivo
        CHECK (motivo IN ('OFENSIVA', 'SPAM', 'DATOS_PERSONALES', 'FALSA', 'OTRO')),
    CONSTRAINT ck_reporte_resenia_estado
        CHECK (estado IN ('PENDIENTE', 'RESUELTO', 'DESCARTADO')),
    CONSTRAINT ck_reporte_resenia_accion
        CHECK (accion IS NULL OR accion IN ('OCULTAR', 'DESCARTAR'))
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_reporte_resenia_resenia'
    ) THEN
        ALTER TABLE reporte_resenia
            ADD CONSTRAINT fk_reporte_resenia_resenia
            FOREIGN KEY (id_resenia) REFERENCES resenia (id_resenia)
            ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_reporte_resenia_reportante'
    ) THEN
        ALTER TABLE reporte_resenia
            ADD CONSTRAINT fk_reporte_resenia_reportante
            FOREIGN KEY (id_reportante) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_reporte_resenia_resuelto_por'
    ) THEN
        ALTER TABLE reporte_resenia
            ADD CONSTRAINT fk_reporte_resenia_resuelto_por
            FOREIGN KEY (id_resuelto_por) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_reporte_resenia_reportante
    ON reporte_resenia (id_resenia, id_reportante);
CREATE INDEX IF NOT EXISTS idx_reporte_resenia_estado
    ON reporte_resenia (estado, fecha_creacion);

-- Índices para las consultas públicas y los promedios.
CREATE INDEX IF NOT EXISTS idx_resenia_estado
    ON resenia (estado, id_pedido);
CREATE INDEX IF NOT EXISTS idx_pedido_servicio
    ON pedido (id_servicio);
