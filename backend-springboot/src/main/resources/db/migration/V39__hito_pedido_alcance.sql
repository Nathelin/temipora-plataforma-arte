-- El hito del pedido guarda el alcance y el porcentaje definidos al presupuestar.
-- El alcance reutiliza la descripción del hito del presupuesto (250 caracteres).

ALTER TABLE hito_pedido ADD COLUMN IF NOT EXISTS descripcion VARCHAR(250);
ALTER TABLE hito_pedido ADD COLUMN IF NOT EXISTS porcentaje NUMERIC(5, 2);

UPDATE hito_pedido h
SET descripcion = hp.descripcion,
    porcentaje = hp.porcentaje
FROM hito_presupuesto hp
WHERE hp.id_hito_presupuesto = h.id_hito_presupuesto
  AND (h.descripcion IS NULL OR h.porcentaje IS NULL);
