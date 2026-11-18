-- Aviso al artista original cuando una pieza suya cambia de dueño por reventa.

INSERT INTO tipo_notificacion (codigo, nombre, categoria, frecuencia, estado)
SELECT 'REVENTA_PRODUCTO', 'Obra revendida', 'PRODUCTOS', 'EVENTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_notificacion WHERE codigo = 'REVENTA_PRODUCTO'
);
