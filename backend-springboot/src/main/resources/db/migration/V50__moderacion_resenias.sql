-- Notificaciones del flujo de moderación de reseñas.
INSERT INTO tipo_notificacion (codigo, nombre, frecuencia, estado)
SELECT v.codigo, v.nombre, 'EVENTO', TRUE
FROM (VALUES
    ('RESENIA_REPORTADA', 'Reseña reportada'),
    ('RESENIA_OCULTA', 'Reseña oculta por moderación'),
    ('RESENIA_RESTAURADA', 'Reseña restaurada')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion t WHERE t.codigo = v.codigo
);
