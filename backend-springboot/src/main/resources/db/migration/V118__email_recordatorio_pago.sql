-- Cola de recordatorios de pago (clientes) y de entrega (artistas).
-- La clave única evita duplicados para el mismo hito y la misma fecha límite.

CREATE TABLE IF NOT EXISTS email_recordatorio_pendiente (
    id_email_recordatorio BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_pedido BIGINT NOT NULL,
    id_hito_pedido BIGINT,
    motivo VARCHAR(30) NOT NULL,
    clave VARCHAR(160) NOT NULL,
    fecha_referencia TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    id_lote VARCHAR(36),
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_en TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_recordatorio_usuario'
    ) THEN
        ALTER TABLE email_recordatorio_pendiente
            ADD CONSTRAINT fk_recordatorio_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_recordatorio_pedido'
    ) THEN
        ALTER TABLE email_recordatorio_pendiente
            ADD CONSTRAINT fk_recordatorio_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_recordatorio_hito'
    ) THEN
        ALTER TABLE email_recordatorio_pendiente
            ADD CONSTRAINT fk_recordatorio_hito
            FOREIGN KEY (id_hito_pedido) REFERENCES hito_pedido (id_hito_pedido);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_recordatorio_clave
    ON email_recordatorio_pendiente (clave);

CREATE INDEX IF NOT EXISTS idx_recordatorio_estado
    ON email_recordatorio_pendiente (estado);

CREATE INDEX IF NOT EXISTS idx_recordatorio_usuario
    ON email_recordatorio_pendiente (id_usuario, estado);

-- El enlace de acceso de los mails de recordatorio usa un propósito nuevo.
ALTER TABLE token_acceso
    DROP CONSTRAINT IF EXISTS ck_token_acceso_proposito;

ALTER TABLE token_acceso
    ADD CONSTRAINT ck_token_acceso_proposito
    CHECK (proposito IN ('OFERTA', 'CIERRE', 'RECORDATORIO'));
