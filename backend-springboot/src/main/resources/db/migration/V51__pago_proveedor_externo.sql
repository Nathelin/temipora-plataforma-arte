ALTER TABLE pago ADD COLUMN IF NOT EXISTS id_pago_externo VARCHAR(100);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS estado_externo VARCHAR(50);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS expira_en TIMESTAMP;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS fecha_actualizacion_proveedor TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pago_proveedor_pago_externo
    ON pago (proveedor, id_pago_externo)
    WHERE id_pago_externo IS NOT NULL;

-- Cuentas de cobro por usuario y proveedor. Se usa en la Fase B (Marketplace)
-- y deja preparada la base para otros proveedores sin migraciones nuevas.
CREATE TABLE IF NOT EXISTS cuenta_pago_usuario (
    id_cuenta_pago BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES usuario(id_usuario),
    proveedor VARCHAR(30) NOT NULL,
    id_cuenta_externa VARCHAR(100),
    access_token TEXT,
    refresh_token TEXT,
    token_expira_en TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_vinculacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT uk_cuenta_pago_usuario UNIQUE (id_usuario, proveedor),
    CONSTRAINT ck_cuenta_pago_estado
        CHECK (estado IN ('PENDIENTE', 'ACTIVA', 'REVOCADA', 'ERROR'))
);
