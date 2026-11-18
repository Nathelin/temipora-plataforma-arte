ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS coleccion_publica BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE compra_item
    ADD COLUMN IF NOT EXISTS oculto_coleccion BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE compra_item
    ADD COLUMN IF NOT EXISTS oculto_coleccion_en TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_compra_item_coleccion
    ON compra_item (id_variante, oculto_coleccion);
