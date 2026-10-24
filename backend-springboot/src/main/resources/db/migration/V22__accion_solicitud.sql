-- UC-19 / UC-22: acciones terminales sobre una solicitud de servicio.
-- Tabla unificada: rechazo (artista) y cancelación (cliente).

CREATE TABLE IF NOT EXISTS accion_solicitud (
    id_accion_solicitud BIGSERIAL PRIMARY KEY,
    id_solicitud BIGINT NOT NULL,
    tipo_accion VARCHAR(20) NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT now(),
    motivo VARCHAR(200)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_accion_solicitud_solicitud') THEN
        ALTER TABLE accion_solicitud ADD CONSTRAINT fk_accion_solicitud_solicitud
            FOREIGN KEY (id_solicitud) REFERENCES solicitud (id_solicitud) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_accion_solicitud_usuario') THEN
        ALTER TABLE accion_solicitud ADD CONSTRAINT fk_accion_solicitud_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_accion_solicitud_tipo') THEN
        ALTER TABLE accion_solicitud ADD CONSTRAINT ck_accion_solicitud_tipo
            CHECK (tipo_accion IN ('RECHAZO', 'CANCELACION'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_accion_solicitud_motivo') THEN
        ALTER TABLE accion_solicitud ADD CONSTRAINT ck_accion_solicitud_motivo
            CHECK (motivo IS NULL OR char_length(motivo) <= 200);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_accion_solicitud_tipo
    ON accion_solicitud (id_solicitud, tipo_accion);
CREATE INDEX IF NOT EXISTS idx_accion_solicitud_fecha
    ON accion_solicitud (id_solicitud, fecha DESC);

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT 'SOLICITUD_RECHAZADA', 'Solicitud de servicio rechazada', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion WHERE codigo = 'SOLICITUD_RECHAZADA'
);
