-- Fase 1: vocabulario inicial de etiquetas (UC-54 a UC-59).
--
-- Siembra las categorías y etiquetas de arranque para dar señal al filtro de
-- servicios y al algoritmo de recomendaciones (similitud Jaccard de etiquetas
-- y de categorías).
--
-- Idempotente y no destructivo: cada inserción va con NOT EXISTS comparando el
-- nombre sin distinguir mayúsculas, así que no pisa ni duplica lo que ya exista
-- (categorías y etiquetas creadas por administradores o propuestas por artistas).
-- Las etiquetas quedan en estado VALIDA y sin creador (son vocabulario del sistema).

-- Categorías. Se incluyen también las ya existentes para que una base limpia
-- (p. ej. CI) quede con el mismo vocabulario; en bases ya migradas son no-ops.
INSERT INTO categoria_etiqueta (nombre, descripcion, observaciones, activo)
SELECT v.nombre, v.descripcion, NULL, TRUE
FROM (VALUES
    ('Estilos de arte',   'Estilos de arte que pueden asociarse a una pieza.'),
    ('Género',            'Describe el universo o ambientación de la pieza.'),
    ('Paleta de colores', 'Paleta de colores utilizada en la pieza.'),
    ('Tema',              'Sujeto o contenido representado en la pieza.')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM categoria_etiqueta c
    WHERE lower(c.nombre) = lower(v.nombre)
);

-- Etiquetas: nombre, descripción y categoría (referida por nombre, sin distinguir
-- mayúsculas). Se agregan a las cuatro categorías.
INSERT INTO etiqueta (
    nombre, descripcion, observaciones, id_categoria,
    id_creado_por, estado, veces_solicitada, id_evaluado_por,
    fecha_creacion, fecha_evaluacion
)
SELECT v.nombre, v.descripcion, NULL, c.id_categoria_etiqueta,
       NULL, 'VALIDA', 0, NULL,
       now(), NULL
FROM (VALUES
    -- Estilos de arte (la categoría ya tiene "anime" y "realismo")
    ('Chibi',               'Personajes de cabeza grande y cuerpo pequeño, proporciones exageradas.',              'Estilos de arte'),
    ('Kawaii',              'Estética tierna y adorable.',                                                       'Estilos de arte'),
    ('Semi-realista',       'Mezcla de anatomía realista con un acabado estilizado.',                            'Estilos de arte'),
    ('Cartoon',             'Dibujo animado occidental, formas simples y exageradas.',                           'Estilos de arte'),
    ('Cómic',               'Estilo propio de la historieta occidental.',                                        'Estilos de arte'),
    ('Stylized',            'Formas y volúmenes estilizados, no realistas.',                                     'Estilos de arte'),
    ('Pixel Art',           'Imagen construida a partir de píxeles.',                                            'Estilos de arte'),
    ('Line art',            'Dibujo definido por líneas, sin color o con color plano.',                          'Estilos de arte'),
    ('Boceto',              'Trazos rápidos y sueltos, sin acabado final.',                                      'Estilos de arte'),
    ('Manga',               'Estilo propio del cómic japonés.',                                                  'Estilos de arte'),
    ('Ukiyo-e',             'Grabado tradicional japonés.',                                                      'Estilos de arte'),

    -- Género (la categoría ya tiene "Cyberpunk" y "romance")
    ('Fantasía',            'Mundos con magia o elementos fantásticos.',                                         'Género'),
    ('Ciencia ficción',     'Tecnología avanzada, futuro o espacio.',                                           'Género'),
    ('Terror',              'Atmósfera de miedo o suspenso.',                                                   'Género'),
    ('Steampunk',           'Ambientación retrofuturista a vapor.',                                             'Género'),
    ('Medieval',            'Ambientación de época medieval.',                                                   'Género'),
    ('Mitología',           'Inspirado en mitos y leyendas.',                                                     'Género'),
    ('Post-apocalíptico',   'Mundo posterior a una catástrofe.',                                                 'Género'),
    ('Espacial',            'Ambientado en el espacio exterior.',                                                'Género'),
    ('Slice of life',       'Escenas cotidianas y costumbristas.',                                               'Género'),
    ('Ambiente escolar',    'Ambientado en un entorno escolar.',                                                 'Género'),
    ('Histórico',           'Recreación de una época histórica.',                                                'Género'),
    ('Western',             'Ambientación del lejano oeste.',                                                     'Género'),
    ('Urbano',              'Ambientado en un entorno urbano moderno.',                                          'Género'),
    ('Distopía',            'Sociedad futura opresiva o decadente.',                                             'Género'),

    -- Paleta de colores (la categoría ya tiene "escala de grises", "neón" y "pastel")
    ('Vibrante',            'Colores intensos y saturados.',                                                     'Paleta de colores'),
    ('Monocromático',       'Variantes de un solo color.',                                                       'Paleta de colores'),
    ('Cálido',              'Dominancia de rojos, naranjas y amarillos.',                                        'Paleta de colores'),
    ('Frío',               'Dominancia de azules, verdes y violetas.',                                          'Paleta de colores'),
    ('Tonos tierra',        'Marrones, ocres y verdes apagados.',                                                'Paleta de colores'),
    ('Duotono',             'Combinación limitada a dos colores.',                                               'Paleta de colores'),
    ('Sepia',               'Tonalidad marrón propia de las fotos antiguas.',                                    'Paleta de colores'),

    -- Tema (categoría nueva, sin etiquetas previas)
    ('Personaje original (OC)', 'Personaje propio del artista, no de una obra existente.',                       'Tema'),
    ('Fanart',              'Representación de un personaje u obra ya existente.',                               'Tema'),
    ('Retrato',             'Enfoque en el rostro o medio cuerpo de un personaje.',                              'Tema'),
    ('Pareja',              'Dos personajes en una misma escena.',                                               'Tema'),
    ('Ilustración grupal',  'Tres o más personajes en una misma escena.',                                        'Tema'),
    ('Paisaje',             'Enfoque en el entorno natural o urbano.',                                            'Tema'),
    ('Naturaleza',          'Elementos naturales: plantas, flores, fauna.',                                      'Tema'),
    ('Animales',            'Animales como sujeto principal.',                                                   'Tema'),
    ('Antropomorfos (Furry)','Animales con rasgos humanos.',                                                     'Tema'),
    ('Kemonomimi',          'Personajes humanos con rasgos animales (orejas, cola).',                             'Tema'),
    ('Mechas',              'Robots gigantes o trajes mecánicos.',                                               'Tema'),
    ('Criaturas',           'Seres fantásticos o monstruos.',                                                    'Tema'),
    ('Arquitectura',        'Edificios y espacios construidos.',                                                  'Tema'),
    ('Bodegón',             'Objetos inanimados en una composición.',                                            'Tema')
) AS v(nombre, descripcion, categoria)
JOIN categoria_etiqueta c
    ON lower(c.nombre) = lower(v.categoria)
WHERE NOT EXISTS (
    SELECT 1 FROM etiqueta e
    WHERE lower(e.nombre) = lower(v.nombre)
);
