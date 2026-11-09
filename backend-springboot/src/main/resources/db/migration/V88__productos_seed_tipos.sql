-- V87: tipos de producto base de UC-01.
-- Idempotente por nombre: si el administrador ya los creó, no se duplican.

INSERT INTO tipo_producto (nombre, descripcion, observaciones, activo)
SELECT v.nombre, v.descripcion, NULL, TRUE
FROM (VALUES
    ('Adoptable',
     'Diseño de personaje original cuyo derecho de uso se vende a un único comprador.'),
    ('Fondo de pantalla PC',
     'Ilustración digital pensada como fondo de pantalla para computadora.'),
    ('Fondo de pantalla móvil',
     'Ilustración digital pensada como fondo de pantalla para teléfono.'),
    ('Íconos de PC/móvil',
     'Conjunto de íconos ilustrados para escritorio o teléfono.'),
    ('Print digital',
     'Ilustración digital preparada para impresión.'),
    ('Foto de perfil',
     'Ilustración digital pensada para usar como foto de perfil.'),
    ('Recurso para streamers',
     'Recursos gráficos para transmisiones en vivo.'),
    ('Recurso para videojuegos',
     'Recursos gráficos para videojuegos.'),
    ('Arte tradicional',
     'Pieza de arte tradicional de una sola unidad; el envío se coordina fuera de la plataforma.'),
    ('Otros',
     'Otros tipos de productos artísticos no listados.')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM tipo_producto t
    WHERE lower(t.nombre) = lower(v.nombre)
);
