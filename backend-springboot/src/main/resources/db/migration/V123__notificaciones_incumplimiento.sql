-- Tipos de notificación de incumplimiento y sanciones (fase 7).

INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT v.codigo, v.nombre, 'PEDIDOS', 'EVENTO', TRUE
FROM (VALUES
    ('ENTREGA_ATRASADA', 'Entrega de pedido atrasada'),
    ('CUENTA_SUSPENDIDA_VENTAS', 'Ventas suspendidas por incumplimiento'),
    ('CUENTA_RESTRINGIDA_ENTREGAS', 'Cuenta restringida a entregas'),
    ('PEDIDO_INCUMPLIDO', 'Pedido cerrado por incumplimiento'),
    ('APELACION_RESUELTA', 'Apelación de sanción resuelta')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
