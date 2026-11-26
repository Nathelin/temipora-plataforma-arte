-- Tipos de notificación de mora del cliente por impago (fase 8).

INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT v.codigo, v.nombre, 'PEDIDOS', 'EVENTO', TRUE
FROM (VALUES
    ('PAGO_ATRASADO', 'Pago de entrega final atrasado'),
    ('CUENTA_SUSPENDIDA_COMPRAS', 'Compras suspendidas por pago pendiente'),
    ('CUENTA_RESTRINGIDA_PAGOS', 'Cuenta limitada a pagos')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
