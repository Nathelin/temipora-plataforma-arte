-- Atribuye a CosmicCrap la autoría de las etiquetas sembradas en V61.
--
-- V61 las insertó sin creador (id_creado_por NULL, estado VALIDA, demanda 0).
-- Sólo se tocan esas filas: se exige creador NULL y que el nombre esté en la
-- lista del seed, de modo que no se pisa ninguna etiqueta creada por admins o
-- artistas, ni una etiqueta homónima que ya existiera antes de V61.
--
-- Idempotente: una segunda corrida no encuentra filas con creador NULL.

UPDATE etiqueta
SET id_creado_por = (
    SELECT id_usuario
    FROM usuario
    WHERE lower(nombre_usuario) = lower('CosmicCrap')
)
WHERE id_creado_por IS NULL
  AND estado = 'VALIDA'
  AND veces_solicitada = 0
  AND nombre IN (
    -- Estilos de arte
    'Chibi', 'Kawaii', 'Semi-realista', 'Cartoon', 'Cómic', 'Stylized',
    'Pixel Art', 'Line art', 'Boceto', 'Manga', 'Ukiyo-e',
    -- Género
    'Fantasía', 'Ciencia ficción', 'Terror', 'Steampunk', 'Medieval',
    'Mitología', 'Post-apocalíptico', 'Espacial', 'Slice of life',
    'Ambiente escolar', 'Histórico', 'Western', 'Urbano', 'Distopía',
    -- Paleta de colores
    'Vibrante', 'Monocromático', 'Cálido', 'Frío', 'Tonos tierra',
    'Duotono', 'Sepia',
    -- Tema
    'Personaje original (OC)', 'Fanart', 'Retrato', 'Pareja',
    'Ilustración grupal', 'Paisaje', 'Naturaleza', 'Animales',
    'Antropomorfos (Furry)', 'Kemonomimi', 'Mechas', 'Criaturas',
    'Arquitectura', 'Bodegón'
  );
