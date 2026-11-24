-- Tipos de notificación para las propuestas de prórroga del plazo.

INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT v.codigo, v.nombre, 'PEDIDOS', 'EVENTO', TRUE
FROM (VALUES
    ('PRORROGA_SOLICITADA', 'Prórroga del plazo solicitada'),
    ('PRORROGA_ACEPTADA', 'Prórroga del plazo aceptada'),
    ('PRORROGA_RECHAZADA', 'Prórroga del plazo rechazada'),
    ('PRORROGA_VENCIDA', 'Prórroga del plazo vencida')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
