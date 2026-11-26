-- Sanciones automáticas por incumplimiento de entrega del artista (fase 7).

-- Mora por pedido: se congela el plazo incumplido, no se recalcula.
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS mora_desde TIMESTAMP;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS incumplimiento_en TIMESTAMP;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS cierre_por_incumplimiento BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pedido_mora
    ON pedido (mora_desde) WHERE mora_desde IS NOT NULL;

-- Sanción: origen, disparador y clave de idempotencia.
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS origen VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS id_pedido BIGINT;
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS id_hito_pedido BIGINT;
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS fecha_objetivo TIMESTAMP;
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS fecha_escalada TIMESTAMP;
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS clave VARCHAR(160);
ALTER TABLE sancion_cuenta ADD COLUMN IF NOT EXISTS alcance_afectado TEXT;

-- Una sanción automática no tiene administrador.
ALTER TABLE sancion_cuenta ALTER COLUMN id_admin_aplico DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_sancion_origen') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT ck_sancion_origen
            CHECK (origen IN ('MANUAL', 'AUTOMATICA'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sancion_cuenta_pedido') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT fk_sancion_cuenta_pedido
            FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sancion_cuenta_hito') THEN
        ALTER TABLE sancion_cuenta ADD CONSTRAINT fk_sancion_cuenta_hito
            FOREIGN KEY (id_hito_pedido) REFERENCES hito_pedido (id_hito_pedido);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sancion_cuenta_clave
    ON sancion_cuenta (clave);

-- Parámetros de la escalada.
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_gracia_entrega INTEGER NOT NULL DEFAULT 3;
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_sancion_ventas INTEGER NOT NULL DEFAULT 14;
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_reapelacion INTEGER NOT NULL DEFAULT 7;
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_cierre_tras_bloqueo INTEGER NOT NULL DEFAULT 30;
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS sanciones_automaticas_desde TIMESTAMP;

-- El corte arranca al aplicar esta migración: los pedidos viejos quedan exentos.
UPDATE configuracion_pedido
    SET sanciones_automaticas_desde = now()
    WHERE sanciones_automaticas_desde IS NULL;
