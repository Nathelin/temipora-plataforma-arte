-- V100: referencia de venta de producto en el ledger.
-- No cambia la fórmula del hash; el id de compra se incluye además en el
-- JSON `datos`, que ya está cubierto por `hash_datos` desde la versión 2.

ALTER TABLE evento_ledger
    ADD COLUMN IF NOT EXISTS id_compra_item BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_evento_ledger_compra_item'
    ) THEN
        ALTER TABLE evento_ledger
            ADD CONSTRAINT fk_evento_ledger_compra_item
            FOREIGN KEY (id_compra_item) REFERENCES compra_item (id_compra_item);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ledger_compra_item
    ON evento_ledger (id_compra_item, tipo);
