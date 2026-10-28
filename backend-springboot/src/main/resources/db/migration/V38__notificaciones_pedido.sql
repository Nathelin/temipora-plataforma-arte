-- Tipos de notificación del ciclo de pedidos y pagos.

INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('PAGO_RECIBIDO', 'Pago de cuota recibido'),
    ('PAGO_RECHAZADO', 'Pago de cuota rechazado'),
    ('CUOTA_PENDIENTE', 'Cuota de pedido habilitada'),
    ('CUOTA_VENCIDA', 'Cuota de pedido vencida'),
    ('HITO_ENTREGADO', 'Entregable de pedido recibido'),
    ('HITO_APROBADO', 'Entregable de pedido aprobado'),
    ('HITO_APROBADO_AUTOMATICAMENTE', 'Entregable aprobado automáticamente'),
    ('HITO_CON_REVISION', 'Revisión de entregable solicitada'),
    ('ENTREGA_LISTA', 'Entrega final lista para pagar'),
    ('PEDIDO_FINALIZADO', 'Pedido finalizado'),
    ('PEDIDO_IMPAGO', 'Pedido cerrado por falta de pago'),
    ('PEDIDO_CANCELADO', 'Pedido cancelado'),
    ('AVANCE_PEDIDO', 'Avance de pedido publicado')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
