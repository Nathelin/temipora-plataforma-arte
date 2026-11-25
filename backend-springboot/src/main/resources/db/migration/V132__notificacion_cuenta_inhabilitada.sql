-- Tipo de notificación para el Nivel 2 (cuenta inhabilitada por incumplimiento).
INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT 'CUENTA_INHABILITADA', 'Cuenta inhabilitada por incumplimiento', 'PEDIDOS', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = 'CUENTA_INHABILITADA'
);
