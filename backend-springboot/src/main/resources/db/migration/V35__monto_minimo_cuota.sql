-- El mínimo por cuota baja de 1000 a 1 para no bloquear pedidos de bajo precio.
-- Sigue siendo configurable desde el panel de admin.
-- Solo se actualiza si el valor sigue siendo el default anterior.

UPDATE configuracion_pedido
SET monto_minimo_cuota = 1.00
WHERE id_configuracion = 1
  AND monto_minimo_cuota = 1000.00;
