-- Infraestructura de email y preferencia de contacto.

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS recibir_emails_promocionales BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS token_baja_email VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_token_baja_email
    ON usuario (token_baja_email)
    WHERE token_baja_email IS NOT NULL;

CREATE TABLE IF NOT EXISTS envio_email (
    id_envio BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT,
    tipo VARCHAR(50) NOT NULL,
    id_referencia BIGINT,
    tipo_referencia VARCHAR(30),
    destinatario VARCHAR(120) NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    proveedor VARCHAR(30),
    id_externo VARCHAR(100),
    error VARCHAR(500),
    clave_idempotencia VARCHAR(120) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_envio TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_envio_email_usuario') THEN
        ALTER TABLE envio_email ADD CONSTRAINT fk_envio_email_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_envio_email_idempotencia') THEN
        ALTER TABLE envio_email ADD CONSTRAINT uk_envio_email_idempotencia
            UNIQUE (clave_idempotencia);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_envio_email_usuario
    ON envio_email (id_usuario, fecha_creacion DESC);
