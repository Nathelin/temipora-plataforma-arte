-- Términos: "qué dibujo" y "qué no dibujo" como texto libre (estilo VGen).
-- Se mantienen las tablas viejas de temas para poder revertir este paso;
-- el DROP se hará en una migración posterior.

ALTER TABLE terminos ADD COLUMN IF NOT EXISTS que_dibuja TEXT;
ALTER TABLE terminos ADD COLUMN IF NOT EXISTS que_no_dibuja TEXT;

-- Backfill: convierte los temas guardados en líneas de texto para no perder
-- la información que ya tenían los artistas.
UPDATE terminos t
SET que_dibuja = COALESCE((
        SELECT string_agg(
                   '- ' || COALESCE(te.nombre, tt.nombre_personalizado),
                   E'\n'
                   ORDER BY COALESCE(te.orden, 9999), COALESCE(te.nombre, tt.nombre_personalizado)
               )
        FROM termino_tema tt
        LEFT JOIN tema_termino te ON te.id_tema_termino = tt.id_tema_termino
        WHERE tt.id_terminos = t.id_terminos AND tt.estado = 'SI'
    ), NULL),
    que_no_dibuja = COALESCE((
        SELECT string_agg(
                   '- ' || COALESCE(te.nombre, tt.nombre_personalizado),
                   E'\n'
                   ORDER BY COALESCE(te.orden, 9999), COALESCE(te.nombre, tt.nombre_personalizado)
               )
        FROM termino_tema tt
        LEFT JOIN tema_termino te ON te.id_tema_termino = tt.id_tema_termino
        WHERE tt.id_terminos = t.id_terminos AND tt.estado = 'NO'
    ), NULL);

-- "Consultar" no tiene un campo propio en el nuevo modelo: se agrega a
-- "qué dibujo" con la aclaración para conservar la intención original.
UPDATE terminos t
SET que_dibuja = COALESCE(t.que_dibuja || E'\n', '') || (
        SELECT string_agg(
                   '- Consultar: ' || COALESCE(te.nombre, tt.nombre_personalizado),
                   E'\n'
                   ORDER BY COALESCE(te.orden, 9999), COALESCE(te.nombre, tt.nombre_personalizado)
               )
        FROM termino_tema tt
        LEFT JOIN tema_termino te ON te.id_tema_termino = tt.id_tema_termino
        WHERE tt.id_terminos = t.id_terminos AND tt.estado = 'CONSULTAR'
    )
WHERE EXISTS (
    SELECT 1 FROM termino_tema tt
    WHERE tt.id_terminos = t.id_terminos AND tt.estado = 'CONSULTAR'
);
