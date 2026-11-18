-- V85: base del módulo de productos.
-- La tabla producto existente fue creada por ddl-auto=update y no tenía filas
-- (verificado antes de la migración), por eso se recrea con el modelo definitivo.

DROP TABLE IF EXISTS producto CASCADE;

CREATE TABLE producto (
    id_producto BIGSERIAL PRIMARY KEY,
    id_artista BIGINT NOT NULL,
    id_tipo_producto BIGINT NOT NULL,
    id_medio BIGINT NOT NULL,
    id_moneda BIGINT NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    contenido_sensible BOOLEAN NOT NULL DEFAULT FALSE,
    permite_reventa BOOLEAN NOT NULL DEFAULT FALSE,
    porcentaje_regalia NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    pausado_en TIMESTAMP,
    archivado_en TIMESTAMP,
    id_archivado_por BIGINT,
    imagen_portada VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_producto_artista FOREIGN KEY (id_artista)
        REFERENCES usuario (id_usuario),
    CONSTRAINT fk_producto_tipo FOREIGN KEY (id_tipo_producto)
        REFERENCES tipo_producto (id_tipo_producto),
    CONSTRAINT fk_producto_medio FOREIGN KEY (id_medio)
        REFERENCES medio (id_medio),
    CONSTRAINT fk_producto_moneda FOREIGN KEY (id_moneda)
        REFERENCES moneda (id_moneda),
    CONSTRAINT fk_producto_archivado_por FOREIGN KEY (id_archivado_por)
        REFERENCES usuario (id_usuario),
    CONSTRAINT ck_producto_estado
        CHECK (estado IN ('DISPONIBLE', 'PAUSADO', 'ARCHIVADO')),
    CONSTRAINT ck_producto_regalia
        CHECK (porcentaje_regalia >= 0 AND porcentaje_regalia <= 10)
);

CREATE UNIQUE INDEX uk_producto_artista_titulo_tipo
    ON producto (id_artista, lower(titulo), id_tipo_producto)
    WHERE estado <> 'ARCHIVADO';

CREATE INDEX idx_producto_artista_estado
    ON producto (id_artista, estado);

CREATE INDEX idx_producto_estado_tipo
    ON producto (estado, id_tipo_producto);

CREATE TABLE producto_variante (
    id_variante BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    precio NUMERIC(12, 2) NOT NULL,
    stock_disponible INTEGER,
    orden INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    imagen_url VARCHAR(255),
    sha256 VARCHAR(64),
    clave_archivo_original VARCHAR(255),
    numero_edicion INTEGER,
    id_propietario_actual BIGINT,
    reservado_hasta TIMESTAMP,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT fk_variante_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_variante_propietario FOREIGN KEY (id_propietario_actual)
        REFERENCES usuario (id_usuario),
    CONSTRAINT ck_variante_estado
        CHECK (estado IN ('DISPONIBLE', 'RESERVADO', 'AGOTADO', 'PAUSADO',
                          'ARCHIVADO', 'EN_REVENTA')),
    CONSTRAINT ck_variante_stock
        CHECK (stock_disponible IS NULL OR stock_disponible >= 0),
    CONSTRAINT ck_variante_precio
        CHECK (precio > 0),
    CONSTRAINT uk_variante_producto_nombre
        UNIQUE (id_producto, nombre),
    CONSTRAINT uk_variante_producto_orden
        UNIQUE (id_producto, orden)
);

CREATE UNIQUE INDEX uk_variante_sha_unica
    ON producto_variante (sha256)
    WHERE stock_disponible = 1
      AND estado <> 'ARCHIVADO'
      AND sha256 IS NOT NULL;

CREATE INDEX idx_variante_producto_estado
    ON producto_variante (id_producto, estado);

CREATE INDEX idx_variante_sha
    ON producto_variante (sha256);

CREATE TABLE producto_imagen (
    id_producto_imagen BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    orden INTEGER NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    clave_archivo_original VARCHAR(255),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_producto_imagen_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE
);

CREATE INDEX idx_producto_imagen_producto
    ON producto_imagen (id_producto, orden);

CREATE INDEX idx_producto_imagen_sha
    ON producto_imagen (sha256);

CREATE TABLE producto_etiqueta (
    id_producto BIGINT NOT NULL,
    id_etiqueta BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_producto, id_etiqueta),
    CONSTRAINT fk_producto_etiqueta_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_producto_etiqueta_etiqueta FOREIGN KEY (id_etiqueta)
        REFERENCES etiqueta (id_etiqueta) ON DELETE CASCADE
);

CREATE TABLE producto_contenido_sensible (
    id_producto BIGINT NOT NULL,
    contenido VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_producto, contenido),
    CONSTRAINT fk_producto_sensible_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE,
    CONSTRAINT ck_producto_sensible
        CHECK (contenido IN ('VIOLENCIA', 'DESNUDOS', 'GENERAL'))
);

-- Los términos pueden ser globales, de un servicio o propios de un producto.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_terminos_producto'
    ) THEN
        ALTER TABLE terminos
            ADD CONSTRAINT fk_terminos_producto FOREIGN KEY (id_producto)
            REFERENCES producto (id_producto);
    END IF;
END $$;
