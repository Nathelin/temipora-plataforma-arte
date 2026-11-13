-- Reseñas de producto: snapshot de las variantes compradas.
--
-- La reseña es por producto, no por variante. Guardamos qué variantes había
-- comprado el autor al momento de reseñar para poder mostrarlo en la reseña.

ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS variantes_compradas VARCHAR(500);
ALTER TABLE resenia
    ADD COLUMN IF NOT EXISTS cantidad_variantes_compradas INTEGER NOT NULL DEFAULT 0;

-- Reseñas de producto previas: al menos la variante del ítem reseñado.
UPDATE resenia r
SET variantes_compradas = ci.nombre_variante,
    cantidad_variantes_compradas = 1
FROM compra_item ci
WHERE r.id_compra_item = ci.id_compra_item
  AND r.variantes_compradas IS NULL;

UPDATE resenia
SET cantidad_variantes_compradas = 0
WHERE cantidad_variantes_compradas IS NULL;
