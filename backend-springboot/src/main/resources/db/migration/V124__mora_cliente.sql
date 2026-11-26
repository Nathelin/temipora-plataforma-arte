-- Mora del cliente por impago de la entrega final (fase 8).

-- Se congela el plazo de pago incumplido, no se recalcula.
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS impago_desde TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_pedido_impago
    ON pedido (impago_desde) WHERE impago_desde IS NOT NULL;

-- Parámetros de la escalada del cliente.
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_gracia_pago INTEGER NOT NULL DEFAULT 3;
ALTER TABLE configuracion_pedido
    ADD COLUMN IF NOT EXISTS dias_sancion_compras INTEGER NOT NULL DEFAULT 27;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_config_cliente_rangos') THEN
        ALTER TABLE configuracion_pedido ADD CONSTRAINT ck_config_cliente_rangos
            CHECK (dias_gracia_pago BETWEEN 1 AND 30
               AND dias_sancion_compras BETWEEN 1 AND 90);
    END IF;
END $$;
