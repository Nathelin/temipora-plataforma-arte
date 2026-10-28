-- Entregas versionadas por hito. Cada entrega es inmutable y conserva su historial;
-- una sola entrega puede quedar vigente por hito. Los archivos se guardan en el
-- almacenamiento privado y la base solo conserva metadatos y hashes.

CREATE TABLE IF NOT EXISTS entrega_hito (
    id_entrega BIGSERIAL PRIMARY KEY,
    id_hito_pedido BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    id_autor BIGINT NOT NULL,
    descripcion VARCHAR(500),
    fecha_entrega TIMESTAMP NOT NULL DEFAULT now(),
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    revision_motivo VARCHAR(500),
    revision_fecha TIMESTAMP,
    revision_por BIGINT,
    retirada BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_retiro VARCHAR(250),
    fecha_retiro TIMESTAMP,
    retirada_por BIGINT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_hito_hito') THEN
        ALTER TABLE entrega_hito ADD CONSTRAINT fk_entrega_hito_hito
            FOREIGN KEY (id_hito_pedido) REFERENCES hito_pedido (id_hito_pedido) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_hito_autor') THEN
        ALTER TABLE entrega_hito ADD CONSTRAINT fk_entrega_hito_autor
            FOREIGN KEY (id_autor) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_hito_revision') THEN
        ALTER TABLE entrega_hito ADD CONSTRAINT fk_entrega_hito_revision
            FOREIGN KEY (revision_por) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_hito_retiro') THEN
        ALTER TABLE entrega_hito ADD CONSTRAINT fk_entrega_hito_retiro
            FOREIGN KEY (retirada_por) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_entrega_hito_numero') THEN
        ALTER TABLE entrega_hito ADD CONSTRAINT ck_entrega_hito_numero
            CHECK (numero > 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_entrega_hito_numero
    ON entrega_hito (id_hito_pedido, numero);
CREATE UNIQUE INDEX IF NOT EXISTS uk_entrega_hito_vigente
    ON entrega_hito (id_hito_pedido) WHERE vigente = TRUE;
CREATE INDEX IF NOT EXISTS idx_entrega_hito_fecha
    ON entrega_hito (id_hito_pedido, fecha_entrega DESC);

CREATE TABLE IF NOT EXISTS entrega_archivo (
    id_archivo BIGSERIAL PRIMARY KEY,
    id_entrega BIGINT NOT NULL,
    id_item_solicitud BIGINT,
    nombre_original VARCHAR(255) NOT NULL,
    clave_original VARCHAR(255) NOT NULL,
    clave_preview VARCHAR(255),
    sha256 CHAR(64) NOT NULL,
    mime VARCHAR(50) NOT NULL,
    ancho INTEGER NOT NULL,
    alto INTEGER NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    retirado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_retiro VARCHAR(250),
    fecha_retiro TIMESTAMP,
    retirado_por BIGINT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_archivo_entrega') THEN
        ALTER TABLE entrega_archivo ADD CONSTRAINT fk_entrega_archivo_entrega
            FOREIGN KEY (id_entrega) REFERENCES entrega_hito (id_entrega) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_archivo_item') THEN
        ALTER TABLE entrega_archivo ADD CONSTRAINT fk_entrega_archivo_item
            FOREIGN KEY (id_item_solicitud) REFERENCES item_solicitud (id_item_solicitud);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_entrega_archivo_retiro') THEN
        ALTER TABLE entrega_archivo ADD CONSTRAINT fk_entrega_archivo_retiro
            FOREIGN KEY (retirado_por) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_entrega_archivo_dimensiones') THEN
        ALTER TABLE entrega_archivo ADD CONSTRAINT ck_entrega_archivo_dimensiones
            CHECK (ancho > 0 AND alto > 0 AND tamano_bytes > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_entrega_archivo_sha') THEN
        ALTER TABLE entrega_archivo ADD CONSTRAINT ck_entrega_archivo_sha
            CHECK (char_length(sha256) = 64);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_entrega_archivo_clave
    ON entrega_archivo (clave_original);
CREATE INDEX IF NOT EXISTS idx_entrega_archivo_entrega
    ON entrega_archivo (id_entrega, orden);
CREATE INDEX IF NOT EXISTS idx_entrega_archivo_item
    ON entrega_archivo (id_item_solicitud);
