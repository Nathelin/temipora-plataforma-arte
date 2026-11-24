-- Tipos de notificación para los recordatorios de pago y entrega.

INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT v.codigo, v.nombre, 'PEDIDOS', 'EVENTO', TRUE
FROM (VALUES
    ('PAGO_POR_VENCER', 'Pago de pedido por vencer'),
    ('ENTREGA_POR_VENCER', 'Entrega de pedido por vencer')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
