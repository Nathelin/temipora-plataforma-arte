-- V101: por ahora cada producto del carrito es una unidad.
-- La columna `cantidad` se conserva para habilitar cantidades en el futuro,
-- pero se normalizan los carritos existentes a 1.

UPDATE carrito_item
SET cantidad = 1
WHERE cantidad <> 1;

COMMENT ON COLUMN carrito_item.cantidad IS
    'Cantidad del ítem. En la v1 siempre es 1; se conserva para el futuro.';
