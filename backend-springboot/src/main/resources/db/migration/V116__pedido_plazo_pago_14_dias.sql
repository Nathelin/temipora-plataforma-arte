-- El plazo máximo para pagar cualquier hito queda acotado a 14 días.
-- Solo cambia la configuración: los pedidos ya creados conservan sus fechas.

UPDATE configuracion_pedido
   SET dias_pago_cuota = 14
 WHERE id_configuracion = 1
   AND dias_pago_cuota > 14;

ALTER TABLE configuracion_pedido
    DROP CONSTRAINT IF EXISTS ck_configuracion_pedido_rangos;

ALTER TABLE configuracion_pedido
    ADD CONSTRAINT ck_configuracion_pedido_rangos
    CHECK (dias_aprobacion_automatica BETWEEN 1 AND 60
       AND dias_pago_cuota BETWEEN 1 AND 14
       AND max_hitos BETWEEN 1 AND 6
       AND porcentaje_max_primer_hito BETWEEN 1 AND 100
       AND monto_minimo_cuota >= 0);
