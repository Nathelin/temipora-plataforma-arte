-- Sanciones de cuenta (v1: bloqueo de acceso).
-- La cuenta nunca se borra: se registra la sanción y se puede revertir.
CREATE TABLE IF NOT EXISTS sancion_cuenta (
    id_sancion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    alcance VARCHAR(20) NOT NULL DEFAULT 'ACCESO',
    motivo VARCHAR(500) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT now(),
    fecha_fin TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    id_admin_aplico BIGINT NOT NULL,
    id_admin_revertio BIGINT,
    fecha_reversion TIMESTAMP,
    observacion_reversion VARCHAR(500)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sancion_cuenta_usuario') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT fk_sancion_cuenta_usuario
            FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sancion_cuenta_admin_aplico') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT fk_sancion_cuenta_admin_aplico
            FOREIGN KEY (id_admin_aplico) REFERENCES usuario (id_usuario);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sancion_cuenta_admin_revertio') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT fk_sancion_cuenta_admin_revertio
            FOREIGN KEY (id_admin_revertio) REFERENCES usuario (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sancion_cuenta_usuario
    ON sancion_cuenta (id_usuario, estado);

CREATE INDEX IF NOT EXISTS idx_sancion_cuenta_fin
    ON sancion_cuenta (fecha_fin);

-- Una sola sanción ACTIVA por usuario (evita carreras entre administradores).
CREATE UNIQUE INDEX IF NOT EXISTS uq_sancion_cuenta_activa
    ON sancion_cuenta (id_usuario) WHERE estado = 'ACTIVA';
