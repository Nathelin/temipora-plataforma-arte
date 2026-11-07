-- Cola de mails de cierre: una fila por cierre y un job que agrupa por
-- usuario para no mandar varios correos seguidos.

CREATE TABLE IF NOT EXISTS email_cierre_pendiente (
    id_email_cierre BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_solicitud BIGINT,
    id_pedido BIGINT,
    motivo VARCHAR(30) NOT NULL,
    fecha_evento TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    id_lote VARCHAR(36),
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT now(),
    enviado_en TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_email_cierre_motivo') THEN
        ALTER TABLE email_cierre_pendiente ADD CONSTRAINT ck_email_cierre_motivo
            CHECK (motivo IN ('SOLICITUD_RECHAZADA','PRESUPUESTO_RETIRADO',
                              'PRESUPUESTO_VENCIDO','SERVICIO_DADO_DE_BAJA',
                              'PEDIDO_IMPAGO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_email_cierre_estado') THEN
        ALTER TABLE email_cierre_pendiente ADD CONSTRAINT ck_email_cierre_estado
            CHECK (estado IN ('PENDIENTE','ENVIADO','OMITIDO','FALLIDO'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_email_cierre_usuario') THEN
        ALTER TABLE email_cierre_pendiente ADD CONSTRAINT fk_email_cierre_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_email_cierre_solicitud') THEN
        ALTER TABLE email_cierre_pendiente ADD CONSTRAINT fk_email_cierre_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_email_cierre_pedido') THEN
        ALTER TABLE email_cierre_pendiente ADD CONSTRAINT fk_email_cierre_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido) ON DELETE CASCADE;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_email_cierre_solicitud
    ON email_cierre_pendiente (id_solicitud, motivo)
    WHERE id_solicitud IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_cierre_pedido
    ON email_cierre_pendiente (id_pedido, motivo)
    WHERE id_pedido IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_email_cierre_estado
    ON email_cierre_pendiente (estado, creado_en);
CREATE INDEX IF NOT EXISTS idx_email_cierre_usuario
    ON email_cierre_pendiente (id_usuario, estado);

ALTER TABLE configuracion_sustitucion
    ADD COLUMN IF NOT EXISTS email_cierre_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_cierre_minutos INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS email_cierre_max INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS email_cierre_vigencia_horas INTEGER NOT NULL DEFAULT 72;

ALTER TABLE configuracion_sustitucion
    DROP CONSTRAINT IF EXISTS ck_configuracion_sustitucion_rango;
ALTER TABLE configuracion_sustitucion
    ADD CONSTRAINT ck_configuracion_sustitucion_rango
    CHECK (max_alternativas BETWEEN 1 AND 20
       AND max_por_artista BETWEEN 1 AND 5
       AND rango_precio_pct BETWEEN 0 AND 200
       AND bonus_favorito BETWEEN 0 AND 1
       AND factor_tiempo_max BETWEEN 1 AND 10
       AND minutos_dedupe BETWEEN 0 AND 1440
       AND dias_retencion >= 0
       AND email_cierre_minutos BETWEEN 0 AND 1440
       AND email_cierre_max BETWEEN 1 AND 20
       AND email_cierre_vigencia_horas BETWEEN 1 AND 720);
