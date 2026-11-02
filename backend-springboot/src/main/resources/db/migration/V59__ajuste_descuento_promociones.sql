-- Alinear el descuento de promociones con el tope de comisión vigente.
-- La configuración no puede superar la comisión; las ofertas ya emitidas
-- también se ajustan para no prometer más de lo que el presupuesto aplica.

UPDATE configuracion_inactividad
SET porcentaje_descuento = LEAST(
    porcentaje_descuento,
    COALESCE(
        (SELECT porcentaje_servicio FROM configuracion_comision WHERE id_configuracion = 1),
        porcentaje_descuento
    )
)
WHERE id_configuracion = 1;

UPDATE oferta_reactivacion o
SET porcentaje_descuento = LEAST(o.porcentaje_descuento, c.porcentaje_servicio)
FROM configuracion_comision c
WHERE c.id_configuracion = 1
  AND o.estado IN ('OFRECIDA', 'RESERVADA')
  AND o.porcentaje_descuento > c.porcentaje_servicio;
