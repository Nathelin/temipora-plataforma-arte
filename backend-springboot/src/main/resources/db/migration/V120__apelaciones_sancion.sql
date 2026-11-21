-- Apelaciones de sanciones automáticas (fase 7).

CREATE TABLE IF NOT EXISTS apelacion_sancion (
    id_apelacion BIGSERIAL PRIMARY KEY,
    id_sancion BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    mensaje TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelta_en TIMESTAMP,
    id_admin_resolvio BIGINT,
    respuesta TEXT
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_apelacion_sancion_sancion') THEN
        ALTER TABLE apelacion_sancion ADD CONSTRAINT fk_apelacion_sancion_sancion
            FOREIGN KEY (id_sancion) REFERENCES sancion_cuenta (id_sancion);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_apelacion_sancion_usuario') THEN
        ALTER TABLE apelacion_sancion ADD CONSTRAINT fk_apelacion_sancion_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_apelacion_sancion_admin') THEN
        ALTER TABLE apelacion_sancion ADD CONSTRAINT fk_apelacion_sancion_admin
            FOREIGN KEY (id_admin_resolvio) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_apelacion_estado') THEN
        ALTER TABLE apelacion_sancion ADD CONSTRAINT ck_apelacion_estado
            CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_apelacion_sancion
    ON apelacion_sancion (id_sancion, creada_en DESC);

-- Una sola apelación abierta por sanción.
CREATE UNIQUE INDEX IF NOT EXISTS uq_apelacion_pendiente
    ON apelacion_sancion (id_sancion) WHERE estado = 'PENDIENTE';
