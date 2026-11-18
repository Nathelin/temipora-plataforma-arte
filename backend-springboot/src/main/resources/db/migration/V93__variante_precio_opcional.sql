-- V93: corrige el NOT NULL de producto_variante.precio.
--
-- V92 relajó el CHECK (precio >= 0) para permitir borradores sin precio, pero
-- la columna seguía siendo NOT NULL, así que insertar una variante de borrador
-- fallaba con "null value in column precio violates not-null constraint".
-- La obligatoriedad del precio se valida en el servicio al publicar.

ALTER TABLE producto_variante
    ALTER COLUMN precio DROP NOT NULL;
