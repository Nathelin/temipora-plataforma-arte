-- Auditoría de descargas de archivos y constancias de pedidos.

CREATE TABLE IF NOT EXISTS descarga_archivo (
    id_descarga BIGSERIAL PRIMARY KEY,
    id_archivo BIGINT,
    id_pedido BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    ip VARCHAR(45),
    fecha TIMESTAMP NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_descarga_archivo') THEN
        ALTER TABLE descarga_archivo ADD CONSTRAINT fk_descarga_archivo
            FOREIGN KEY (id_archivo) REFERENCES entrega_archivo (id_archivo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_descarga_pedido') THEN
        ALTER TABLE descarga_archivo ADD CONSTRAINT fk_descarga_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_descarga_usuario') THEN
        ALTER TABLE descarga_archivo ADD CONSTRAINT fk_descarga_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_descarga_tipo') THEN
        ALTER TABLE descarga_archivo ADD CONSTRAINT ck_descarga_tipo
            CHECK (tipo IN ('PREVIEW', 'ORIGINAL', 'ZIP', 'CONSTANCIA'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_descarga_pedido
    ON descarga_archivo (id_pedido, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_descarga_archivo
    ON descarga_archivo (id_archivo, fecha DESC);
