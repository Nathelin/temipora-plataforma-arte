-- Quita campos que no se usan de los términos del artista:
-- "tiempo de respuesta" (irrelevante) y la sección libre "notas".

ALTER TABLE terminos DROP COLUMN IF EXISTS tiempo_respuesta;
ALTER TABLE terminos DROP COLUMN IF EXISTS notas;
