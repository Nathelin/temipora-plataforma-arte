-- Fase A: sistema de etiquetas (UC-54 a UC-59)
-- Categorías de etiquetas (solo administradores, baja lógica)
CREATE TABLE IF NOT EXISTS categoria_etiqueta (
    id_categoria_etiqueta BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_categoria_etiqueta_nombre'
    ) THEN
        ALTER TABLE categoria_etiqueta
            ADD CONSTRAINT uk_categoria_etiqueta_nombre
            UNIQUE (nombre);
    END IF;
END $$;

-- Etiquetas: workflow PENDIENTE / VALIDA / RECHAZADA / INACTIVA
CREATE TABLE IF NOT EXISTS etiqueta (
    id_etiqueta BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    observaciones VARCHAR(500),
    id_categoria BIGINT NOT NULL,
    id_creado_por BIGINT,
    estado VARCHAR(20) NOT NULL,
    veces_solicitada INTEGER NOT NULL DEFAULT 0,
    id_evaluado_por BIGINT,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_evaluacion TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_etiqueta_nombre'
    ) THEN
        ALTER TABLE etiqueta
            ADD CONSTRAINT uk_etiqueta_nombre
            UNIQUE (nombre);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_etiqueta_categoria'
    ) THEN
        ALTER TABLE etiqueta
            ADD CONSTRAINT fk_etiqueta_categoria
            FOREIGN KEY (id_categoria) REFERENCES categoria_etiqueta (id_categoria_etiqueta);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_etiqueta_creado_por'
    ) THEN
        ALTER TABLE etiqueta
            ADD CONSTRAINT fk_etiqueta_creado_por
            FOREIGN KEY (id_creado_por) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_etiqueta_evaluado_por'
    ) THEN
        ALTER TABLE etiqueta
            ADD CONSTRAINT fk_etiqueta_evaluado_por
            FOREIGN KEY (id_evaluado_por) REFERENCES usuario (id_usuario);
    END IF;
END $$;

-- Intermedia imagen <-> etiqueta (la asignación llega en la Fase C)
CREATE TABLE IF NOT EXISTS imagen_etiqueta (
    id_imagen_etiqueta BIGSERIAL PRIMARY KEY,
    id_imagen BIGINT NOT NULL,
    id_etiqueta BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_imagen_etiqueta'
    ) THEN
        ALTER TABLE imagen_etiqueta
            ADD CONSTRAINT uk_imagen_etiqueta
            UNIQUE (id_imagen, id_etiqueta);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_imagenetiqueta_imagen'
    ) THEN
        ALTER TABLE imagen_etiqueta
            ADD CONSTRAINT fk_imagenetiqueta_imagen
            FOREIGN KEY (id_imagen) REFERENCES imagen (id_imagen);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_imagenetiqueta_etiqueta'
    ) THEN
        ALTER TABLE imagen_etiqueta
            ADD CONSTRAINT fk_imagenetiqueta_etiqueta
            FOREIGN KEY (id_etiqueta) REFERENCES etiqueta (id_etiqueta);
    END IF;
END $$;
