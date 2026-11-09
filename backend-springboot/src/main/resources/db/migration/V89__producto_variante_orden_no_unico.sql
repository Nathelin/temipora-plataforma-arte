-- V89: el orden de las variantes se valida en el servicio, pero no se
-- impone como UNIQUE en la base porque reordenar dos variantes existentes
-- puede violar la constraint durante los UPDATE intermedios.

ALTER TABLE producto_variante
    DROP CONSTRAINT IF EXISTS uk_variante_producto_orden;
